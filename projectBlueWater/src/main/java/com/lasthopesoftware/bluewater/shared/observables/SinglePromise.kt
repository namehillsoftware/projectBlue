package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable

class SinglePromise<T> private constructor(private val promise: Promise<T>) :
	Single<T>(),
	Disposable
{
	@Volatile
	private var isCancelled = false

	override fun subscribeActual(observer: SingleObserver<in T>) {
		observer.onSubscribe(this)
		promise
			.then(
				observer::onSuccess,
				observer::onError)
	}

	override fun dispose() {
		isCancelled = true
		promise.cancel()
	}

	override fun isDisposed(): Boolean = isCancelled

	companion object {
		fun <T> Promise<T>.toSingle(): Single<T> = SinglePromise(this)
	}
}
