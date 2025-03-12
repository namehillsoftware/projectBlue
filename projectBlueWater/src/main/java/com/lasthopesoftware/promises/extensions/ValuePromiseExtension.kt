package com.lasthopesoftware.promises.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateCancellableResponse
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

infix fun <Resolution, Response> Promise<Resolution>.then(response: ImmediateResponse<Resolution, Response>): Promise<Response> =
	this.then(response)

infix fun <Resolution, Response> Promise<Resolution>.excuse(response: ImmediateResponse<Throwable, Response>): Promise<Response> =
	this.excuse(response)

fun <Resolution> Executor.preparePromise(messageWriter: CancellableMessageWriter<Resolution>) = QueuedCancellablePromise(messageWriter, this)

@Composable
fun <T> Promise<T>.toState(initialValue: T): State<T> = produceState(initialValue) {
	value = suspend()
}

@Composable
fun <T> Promise<T>.toState(initialValue: T, key1: Any?): State<T> = produceState(initialValue, key1) {
	value = suspend()
}

suspend fun <T> Promise<T>.suspend(): T = suspendCancellableCoroutine { d ->
	d.invokeOnCancellation { cancel() }

	then(d::resume, d::resumeWithException)
}

fun Completable.toPromise(): Promise<Unit> = CompletablePromise(this)

fun <T : Any> Observable<T>.promiseFirstResult(): Promise<T> = ObserverPromise(this)

fun <T> ListenableFuture<T>.toPromise(executor: Executor): Promise<T> = PromisedListenableFuture(this, executor)

fun <T> Promise<T>.toListenableFuture(): ListenableFuture<T> = SettableFuture.create<T>().apply {
	then(::set, ::setException)
}

fun Job.toPromise(): Promise<Unit> = PromiseJob(this)

@ExperimentalCoroutinesApi
fun <T> Deferred<T>.toPromise(): Promise<T> = PromiseDeferred(this)

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	null -> Promise.empty()
	is Unit -> UnitPromise as Promise<T>
	is Boolean -> (if (this) TruePromise else FalsePromise) as Promise<T>
	else -> Promise(this)
}

inline fun <T, P> Promise<T>.cancelBackThen(crossinline response: (T, CancellationSignal) -> P): Promise<P> =
	object : Promise.Proxy<P>(), ImmediateCancellableResponse<T, P> {
		init {
			proxy(
				this@cancelBackThen
					.also(::doCancel)
					.then(this)
			)
		}

		override fun respond(resolution: T, c: CancellationSignal): P = response(resolution, c)
	}

inline fun <T, P> Promise<T>.cancelBackEventually(crossinline response: (T) -> Promise<P>): Promise<P> =
	object : Promise.Proxy<P>(), PromisedResponse<T, P> {
		init {
			proxy(
				this@cancelBackEventually
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(resolution: T): Promise<P> = response(resolution)
	}

private object TruePromise : Promise<Boolean>(true)
private object FalsePromise: Promise<Boolean>(false)

private object UnitPromise : Promise<Unit>(Unit)

@Suppress("UNCHECKED_CAST")
fun <T> Promise<T>?.keepPromise(): Promise<T?> = this as? Promise<T?> ?: Promise.empty<T?>()

fun <T> Promise<T>?.keepPromise(default: T): Promise<T> = this ?: default.toPromise()

fun <T> Promise<T>.unitResponse(): Promise<Unit> = this.then(UnitResponse.respond())

fun <T> Promise<T>.guaranteedUnitResponse(): Promise<Unit> = this.then(
	UnitResponse.respond(),
	UnitResponse.respond()
)

private class UnitResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit> {
	override fun respond(resolution: Resolution) = Unit

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
		job.invokeOnCompletion(this)
		awaitCancellation(this)
	}

	override fun cancellationRequested() {
		job.cancel()
	}

	override fun invoke(cause: Throwable?) {
		if (cause == null) resolve(Unit)
		else reject(cause)
	}
}

@ExperimentalCoroutinesApi
private class PromiseDeferred<T>(private val deferred: Deferred<T>) : Promise<T>(), CancellationResponse, CompletionHandler {
	init {
		deferred.invokeOnCompletion(this)
		awaitCancellation(this)
	}

	override fun cancellationRequested() {
		deferred.cancel()
	}

	override fun invoke(cause: Throwable?) {
		if (cause == null) resolve(deferred.getCompleted())
		else reject(cause)
	}
}
