package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.google.common.util.concurrent.ListenableFuture
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.disposables.Disposable
import java.util.concurrent.Executor

fun Completable.toPromise(): Promise<Unit> = CompletablePromise(this)

fun <T> ListenableFuture<T>.toPromise(executor: Executor): Promise<T> = PromisedListenableFuture(this, executor)

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	is Unit -> UnitPromise as Promise<T>
	null -> Promise.empty()
	else -> Promise(this)
}

private object UnitPromise : Promise<Unit>(Unit)

fun <T> Promise<T?>?.keepPromise(): Promise<T?> = this ?: Promise.empty()

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
	}

	override fun run() {
		disposable.dispose()
	}

	override fun onSubscribe(d: Disposable) {
		disposable = d
	}

	override fun onComplete() {
		resolve(Unit)
	}

	override fun onError(e: Throwable) {
		reject(e)
	}
}

private class PromisedListenableFuture<Resolution>(private val listenableFuture: ListenableFuture<Resolution>, executor: Executor) : Promise<Resolution>() {
	init {
		respondToCancellation { listenableFuture.cancel(false) }
		listenableFuture.addListener({
			try {
				resolve(listenableFuture.get())
			} catch (t: Throwable) {
				reject(t)
			}
		}, executor)
	}
}
