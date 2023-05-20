package com.lasthopesoftware.bluewater.shared.promises.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.google.common.util.concurrent.ListenableFuture
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun <T> Promise<T>.toState(initialValue: T): State<T> = produceState(initialValue) {
	value = suspend()
}

suspend fun <T> Promise<T>.suspend(): T = suspendCancellableCoroutine { d ->
	d.invokeOnCancellation { cancel() }

	then(d::resume, d::resumeWithException)
}

fun Completable.toPromise(): Promise<Unit> = CompletablePromise(this)

fun <T> ListenableFuture<T>.toPromise(executor: Executor): Promise<T> = PromisedListenableFuture(this, executor)

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

private object TruePromise : Promise<Boolean>(true)
private object FalsePromise: Promise<Boolean>(false)

private object UnitPromise : Promise<Unit>(Unit)

fun <T> Promise<T>?.keepPromise(): Promise<T?> = this?.forward() ?: Promise.empty()

fun <T> Promise<T>?.keepPromise(default: T): Promise<T> = this ?: default.toPromise()

fun <T> Promise<T>.unitResponse(): Promise<Unit> = this.then(UnitResponse.respond())

fun <T> Promise<T>.guaranteedUnitResponse(): Promise<Unit> = this.then(UnitResponse.respond(), UnitResponse.respond())

private class UnitResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit> {
	override fun respond(resolution: Resolution) = Unit

	companion object {
		private val singleUnitResponse by lazy { UnitResponse<Any>() }

		@Suppress("UNCHECKED_CAST")
		fun <Resolution> respond(): UnitResponse<Resolution> = singleUnitResponse as UnitResponse<Resolution>
	}
}

private class CompletablePromise(completable: Completable) : Promise<Unit>(), CompletableObserver, Runnable {
	private lateinit var disposable: Disposable

	init {
		completable.subscribe(this)
		respondToCancellation(this)
	}

	override fun run() {
		disposable.dispose()
	}

	override fun onSubscribe(d: Disposable) {
		disposable = d
	}

	override fun onComplete() = resolve(Unit)

	override fun onError(e: Throwable) = reject(e)
}

private class PromisedListenableFuture<Resolution>(listenableFuture: ListenableFuture<Resolution>, executor: Executor) :
	Promise<Resolution>() {
	init {
		respondToCancellation { listenableFuture.cancel(false) }
		listenableFuture.addListener({
			try {
				resolve(listenableFuture.get())
			} catch (e: ExecutionException) {
				reject(e.cause ?: e)
			} catch (t: Throwable) {
				reject(t)
			}
		}, executor)
	}
}

private class PromiseJob(private val job: Job) : Promise<Unit>(), Runnable, CompletionHandler {
	init {
		job.invokeOnCompletion(this)
		respondToCancellation(this)
	}

	override fun run() {
		job.cancel()
	}

	override fun invoke(cause: Throwable?) {
		if (cause == null) resolve(Unit)
		else reject(cause)
	}
}

@ExperimentalCoroutinesApi
private class PromiseDeferred<T>(private val deferred: Deferred<T>) : Promise<T>(), Runnable, CompletionHandler {
	init {
		deferred.invokeOnCompletion(this)
		respondToCancellation(this)
	}

	override fun run() {
		deferred.cancel()
	}

	override fun invoke(cause: Throwable?) {
		if (cause == null) resolve(deferred.getCompleted())
		else reject(cause)
	}
}
