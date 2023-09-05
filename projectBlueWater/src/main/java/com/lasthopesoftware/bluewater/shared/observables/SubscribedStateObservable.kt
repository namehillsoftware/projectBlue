package com.lasthopesoftware.bluewater.shared.observables

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.BehaviorSubject

class SubscribedStateObservable<T: Any>(source: Observable<T>, private val initialValue: T) : ReadOnlyStateObservable<T>(), AutoCloseable {

	private val behaviorSubject = BehaviorSubject.createDefault(initialValue)
	private val subscription = source.distinctUntilChanged().subscribe { behaviorSubject.onNext(it) }

	override val value: T
		get() = behaviorSubject.value ?: initialValue

	override fun subscribeActual(observer: Observer<in T>?) {
		behaviorSubject.safeSubscribe(observer)
	}

	override fun close() {
		subscription.dispose()
	}
}
