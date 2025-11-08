package com.lasthopesoftware.promises.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

infix fun <Resolution, Response> Promise<Resolution>.then(response: ImmediateResponse<Resolution, Response>): Promise<Response> =
	this.then(response)

infix fun <Resolution, Response> Promise<Resolution>.excuse(response: ImmediateResponse<Throwable, Response>): Promise<Response> =
	this.excuse(response)

fun <Resolution> Executor.preparePromise(messageWriter: CancellableMessageWriter<Resolution>) = QueuedCancellablePromise(messageWriter, this)

// ListenableFuture has unique cancellation characteristics in that it overrides cancellation handling of the underlying
// setting function, which is not a good idea. So this uses a custom, simpler FuturePromise implementation.
fun <Resolution> Promise<Resolution>.toFuture(): Future<Resolution?> = FuturePromise(this)

// Get the result in less time than the Application Not Responding error from Android
fun <Resolution> Future<Resolution>.getSafely(): Resolution? = get(3, TimeUnit.SECONDS)

@Composable
fun <T> Promise<T>.toState(initialValue: T): State<T> = toState(initialValue, Unit)

@Composable
fun <T> Promise<T>.toState(initialValue: T, key1: Any?): State<T> {
	val result = remember { mutableStateOf(initialValue) }
	DisposableEffect(key1) {
		val promisedSet = then { it, cs ->
			if (!cs.isCancelled)
				result.value = it
		}

		onDispose {
			promisedSet.cancel()
		}
	}
	return result
}

suspend fun <T> Promise<T>.suspend(): T = suspendCancellableCoroutine { d ->
	d.invokeOnCancellation { cancel() }

	then(d::resume, d::resumeWithException)
}

fun Completable.toPromise(): Promise<Unit> = CompletablePromise(this)

fun <T : Any> Observable<T>.promiseFirstResult(): Promise<T> = ObserverPromise(this)

fun <T> ListenableFuture<T>.toPromise(executor: Executor): Promise<T> = PromisedListenableFuture(this, executor)

fun <T> Promise<T>.toListenableFuture(): ListenableFuture<T> = SettableFuture.create<T>().also {
	then(SettableFutureResolvedResponse(it), SettableFutureRejectedResponse(it))
}

fun Job.toPromise(): Promise<Unit> = PromiseJob(this)

fun <T> Deferred<T>.toPromise(): Promise<T> = PromiseDeferred(this)

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	null -> Promise.empty()
	is Unit -> toPromise() as Promise<T>
	is Int -> toPromise() as Promise<T>
	is Boolean -> toPromise() as Promise<T>
	is String -> if (isEmpty()) EmptyStringPromise as Promise<T> else Promise(this)
	else -> Promise(this)
}

fun Unit.toPromise(): Promise<Unit> = UnitPromise

fun Int.toPromise(): Promise<Int> = when (this) {
	0 -> ZeroPromise
	1 -> OnePromise
	-1 -> NegativeOnePromise
	else -> Promise(this)
}

fun Boolean.toPromise(): Promise<Boolean> = if (this) TruePromise else FalsePromise

private object ZeroPromise : Promise<Int>(0)
private object OnePromise : Promise<Int>(1)
private object NegativeOnePromise : Promise<Int>(-1)
private object TruePromise : Promise<Boolean>(true)
private object FalsePromise: Promise<Boolean>(false)
private object EmptyStringPromise: Promise<String>("")
private object UnitPromise : Promise<Unit>(Unit)

@Suppress("UNCHECKED_CAST")
fun <T> Promise<T>?.keepPromise(): Promise<T?> = this as? Promise<T?> ?: Promise.empty<T?>()

fun <T> Promise<T>?.keepPromise(default: T): Promise<T> = this ?: default.toPromise()

inline fun <T> Promise<T>?.keepPromise(defaultFactory: () -> T): Promise<T> = this ?: defaultFactory().toPromise()

fun <T> Promise<T>.unitResponse(): Promise<Unit> = this.then(UnitResponse.respond())

fun <T> Promise<T>.guaranteedUnitResponse(): Promise<Unit> = this.then(
	UnitResponse.respond(),
	UnitResponse.respond()
)

class UnitResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit>, (Resolution, CancellationSignal) -> Unit {
	override fun respond(resolution: Resolution) = Unit

	override fun invoke(p1: Resolution, p2: CancellationSignal) = Unit

	companion object {

		private val singleUnitResponse by lazy { UnitResponse<Any>() }
		@Suppress("UNCHECKED_CAST")
		fun <Resolution> respond(): UnitResponse<Resolution> = singleUnitResponse as UnitResponse<Resolution>
	}
}

private class CompletablePromise(completable: Completable) : Promise<Unit>(), CompletableObserver, CancellationResponse {
	private lateinit var disposable: Disposable

	init {
		completable.subscribe(this)
		awaitCancellation(this)
	}

	override fun cancellationRequested() {
		disposable.dispose()
	}

	override fun onSubscribe(d: Disposable) {
		disposable = d
	}

	override fun onComplete() = resolve(Unit)

	override fun onError(e: Throwable) = reject(e)
}

private class ObserverPromise<T : Any>(observable: Observable<T>) : Promise<T>(), Observer<T>, CancellationResponse {
	private lateinit var disposable: Disposable

	init {
		observable.subscribe(this)
		awaitCancellation(this)
	}

	override fun onNext(t: T) {
		resolve(t)
		disposable.dispose()
	}

	override fun onComplete() {
		reject(IllegalStateException("Observable was completed before a result was received."))
		disposable.dispose()
	}

	override fun cancellationRequested() {
		disposable.dispose()
	}

	override fun onSubscribe(d: Disposable) {
		disposable = d
	}

	override fun onError(e: Throwable) = reject(e)
}

private class PromisedListenableFuture<Resolution>(private val listenableFuture: ListenableFuture<Resolution>, executor: Executor) :
	Promise<Resolution>(), CancellationResponse, Runnable {
	init {
		awaitCancellation(this)
		listenableFuture.addListener(this, executor)
	}

	override fun run() {
		try {
			resolve(listenableFuture.get())
		} catch (e: ExecutionException) {
			reject(e.cause ?: e)
		} catch (t: Throwable) {
			reject(t)
		}
	}

	override fun cancellationRequested() {
		listenableFuture.cancel(false)
	}
}

private class PromiseJob(private val job: Job) : Promise<Unit>(), CancellationResponse, CompletionHandler {
	init {
		awaitCancellation(this)
		job.invokeOnCompletion(this)
	}

	override fun cancellationRequested() {
		job.cancel()
	}

	override fun invoke(cause: Throwable?) {
		if (cause == null) resolve(Unit)
		else reject(cause)
	}
}

private class PromiseDeferred<T>(private val deferred: Deferred<T>) : Promise<T>(), CancellationResponse, CompletionHandler {
	init {
		deferred.invokeOnCompletion(this)
		awaitCancellation(this)
	}

	override fun cancellationRequested() {
		deferred.cancel()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun invoke(cause: Throwable?) {
		if (cause == null) resolve(deferred.getCompleted())
		else reject(cause)
	}
}

private class FuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val cancellationProxy = CancellationProxy()
	private val countDownLatch = CountDownLatch(1)
	private val message = AtomicReference<Pair<Resolution?, Throwable?>?>()

	init {
		cancellationProxy.doCancel(promise)
		promise
			.then({ r ->
				if (message.compareAndSet(null, Pair(r, null)))
					countDownLatch.countDown()
			}, { e ->
				if (message.compareAndSet(null, Pair(null, e)))
					countDownLatch.countDown()
			})
	}

	override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
		if (isDone) return false
		cancellationProxy.cancellationRequested()
		return true
	}

	override fun isCancelled(): Boolean = cancellationProxy.isCancelled

	override fun isDone(): Boolean = message.get() != null

	override fun get(): Resolution? {
		countDownLatch.await()
		return getResolution()
	}

	override fun get(timeout: Long, unit: TimeUnit): Resolution? {
		if (countDownLatch.await(timeout, unit)) return getResolution()
		throw TimeoutException("Timed out waiting $timeout $unit for promise to resolve")
	}

	private fun getResolution(): Resolution? = message.get()?.let { (resolution, rejection) ->
		if (rejection != null)
			throw ExecutionException(rejection)
		resolution
	}
}

private class SettableFutureResolvedResponse<Resolution>(private val future: SettableFuture<Resolution>): ImmediateResponse<Resolution, Unit> {
	@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
	override fun respond(resolution: Resolution) {
		future.set(resolution)
	}
}

private class SettableFutureRejectedResponse<Resolution>(private val future: SettableFuture<Resolution>): ImmediateResponse<Throwable, Unit> {
	override fun respond(rejection: Throwable) {
		future.setException(rejection)
	}
}
