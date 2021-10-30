package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class ObservedPromise<T> private constructor(private val promise: Promise<T>) :
	Observable<T>(),
	Disposable
{
	@Volatile
	private var isCancelled = false

	override fun subscribeActual(observer: Observer<in T>) {
		observer.onSubscribe(this)
		promise
			.then(
				{ t ->
					observer.onNext(t)
					observer.onComplete()
				},
				observer::onError)
	}

	override fun dispose() {
		isCancelled = true
		promise.cancel()
	}

	override fun isDisposed(): Boolean = isCancelled

	companion object {
		fun <T> Promise<T>.observe(): Observable<T> = ObservedPromise(this)
	}
}
