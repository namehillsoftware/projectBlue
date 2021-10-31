package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.reactivex.disposables.Disposable

fun <T> Promise<T>.toMaybeObservable(): Maybe<T> = MaybePromise(this)

private class MaybePromise<T>(private val promise: Promise<T>) :
	Maybe<T>(),
	Disposable
{
	@Volatile
	private var isCancelled = false

	override fun subscribeActual(observer: MaybeObserver<in T>) {
		observer.onSubscribe(this)
		promise
			.then(
				{ t -> if (t != null) observer.onSuccess(t) else observer.onComplete() },
				observer::onError)
	}

	override fun dispose() {
		isCancelled = true
		promise.cancel()
	}

	override fun isDisposed(): Boolean = isCancelled
}
