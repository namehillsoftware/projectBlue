package com.lasthopesoftware.bluewater.shared.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.BehaviorSubject

class SubscribedStateObservable<T: Any>(source: Observable<T>, private val initialValue: T) : ReadOnlyStateObservable<T>(), AutoCloseable {

	private val behaviorSubject = BehaviorSubject.createDefault(NullBox(initialValue))
	private val subscription = source.distinctUntilChanged().subscribe { behaviorSubject.onNext(NullBox(it)) }

	override val value: T
		get() = behaviorSubject.value?.value ?: initialValue

	override fun subscribeActual(observer: Observer<in NullBox<T>>?) {
		behaviorSubject.safeSubscribe(observer)
	}

	override fun close() {
		subscription.dispose()
	}
}
