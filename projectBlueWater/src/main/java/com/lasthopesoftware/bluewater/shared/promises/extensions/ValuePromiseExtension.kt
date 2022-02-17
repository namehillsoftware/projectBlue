package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.google.common.util.concurrent.ListenableFuture
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

fun <T> Promise<T>.toDeferred(): Deferred<T> = CompletableDeferred<T>().also {
	then(it::complete, it::completeExceptionally)
}

fun Completable.toPromise(): Promise<Unit> = CompletablePromise(this)

fun <T> ListenableFuture<T>.toPromise(executor: Executor): Promise<T> = PromisedListenableFuture(this, executor)

fun Job.toPromise(): Promise<Unit> = PromiseJob(this)

@ExperimentalCoroutinesApi
fun <T> Deferred<T>.toPromise(): Promise<T> = PromiseDeferred(this)

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	is Unit -> UnitPromise as Promise<T>
	null -> Promise.empty()
	else -> Promise(this)
}

private object UnitPromise : Promise<Unit>(Unit)

fun <T> Promise<T>?.keepPromise(): Promise<T?> = this?.forward() ?: Promise.empty()

fun <T> Promise<T>?.keepPromise(default: T): Promise<T> = this ?: default.toPromise()

fun <T> Promise<T>.unitResponse(): Promise<Unit> = this.then(UnitResponse.respond())

private class UnitResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit> {
	override fun respond(resolution: Resolution) {}

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

private class PromisedListenableFuture<Resolution>(listenableFuture: ListenableFuture<Resolution>, executor: Executor) : Promise<Resolution>() {
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

private class PromiseJob(private val job: Job) : Promise<Unit>(), Runnable {
	init {
	    job.invokeOnCompletion { e ->
			if (e == null) resolve(Unit)
			else reject(e)
		}

		respondToCancellation(this)
	}

	override fun run() {
		job.cancel()
	}
}

@ExperimentalCoroutinesApi
private class PromiseDeferred<T>(private val deferred: Deferred<T>): Promise<T>(), Runnable {
	init {
	    deferred.invokeOnCompletion { e ->
			if (e == null) resolve(deferred.getCompleted())
			else reject(e)
		}

		respondToCancellation(this)
	}

	override fun run() {
		deferred.cancel()
	}
}
