package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.reactivex.disposables.Disposable

class MaybePromise<T> private constructor(private val promise: Promise<T>) :
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

	companion object {
		fun <T> Promise<T>.toMaybe(): Maybe<T> = MaybePromise(this)
	}
}
