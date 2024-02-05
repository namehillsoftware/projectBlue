package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeObserver
import io.reactivex.rxjava3.disposables.Disposable

fun <T : Any> Promise<T>.toMaybeObservable(): Maybe<T> = MaybeObservePromise(this)

private class MaybeObservePromise<T : Any>(private val promise: Promise<T>) :
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
