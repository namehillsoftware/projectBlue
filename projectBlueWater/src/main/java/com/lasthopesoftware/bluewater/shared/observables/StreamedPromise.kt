package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class StreamedPromise<T, S : Iterable<T>> private constructor(private val promise: Promise<S>) :
	Observable<T>(), Disposable
{
	@Volatile
	private var isCancelled = false

	override fun subscribeActual(observer: Observer<in T>) {
		observer.onSubscribe(this)
		promise
			.then(
				{ ts ->
					for (t in ts) observer.onNext(t)
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
		fun <T, S : Iterable<T>> Promise<S>.stream(): Observable<T> = StreamedPromise(this)
	}
}
