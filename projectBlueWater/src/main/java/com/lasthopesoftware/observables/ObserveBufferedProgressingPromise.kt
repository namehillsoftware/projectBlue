package com.lasthopesoftware.observables

import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.onEach
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.UnicastSubject

fun <T : Any> ProgressingPromise<T, *>.observeBufferedProgress(): Observable<T> = ObserveBufferedProgressingPromise(this)

private class ObserveBufferedProgressingPromise<T : Any>(progressingPromise: ProgressingPromise<T, *>) : Observable<T>(), Disposable {
	private val cancellationProxy = CancellationProxy()
	private val subject = UnicastSubject.create<T>(true)

	init {
	    cancellationProxy.doCancel(progressingPromise)
		progressingPromise
			.onEach { subject.onNext(it) }
			.then({ subject.onComplete() }, subject::onError)
	}

	override fun subscribeActual(observer: Observer<in T>) {
		subject.subscribe(observer)
		observer.onSubscribe(this)
	}

	override fun dispose() = cancellationProxy.cancellationRequested()

	override fun isDisposed(): Boolean = cancellationProxy.isCancelled
}
