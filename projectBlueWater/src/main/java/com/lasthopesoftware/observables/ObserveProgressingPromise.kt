package com.lasthopesoftware.observables

import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.onEach
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

fun <T : Any> ProgressingPromise<T, *>.observeProgress(): Observable<T> = ObserveProgressingPromise(this)

private class ObserveProgressingPromise<T : Any>(private val progressingPromise: ProgressingPromise<T, *>) : Observable<T>(), Disposable {
	private val cancellationProxy = CancellationProxy()

	init {
	    cancellationProxy.doCancel(progressingPromise)
	}

	override fun subscribeActual(observer: Observer<in T>) {
		observer.onSubscribe(this)
		progressingPromise
			.onEach { observer.onNext(it) }
			.then(
				{ observer.onComplete() },
				{ observer.onError(it) }
			)
	}

	override fun dispose() {
		cancellationProxy.cancel()
	}

	override fun isDisposed(): Boolean = cancellationProxy.isCancelled

}
