package com.lasthopesoftware.bluewater.shared.observables

import io.reactivex.Observer
import io.reactivex.subjects.BehaviorSubject

class MutableStateObservable<T : Any>(private val initialValue: T) : ReadOnlyStateObservable<T>() {

	private val behaviorSubject = BehaviorSubject.createDefault(initialValue)

	override var value: T
		get() = behaviorSubject.value ?: initialValue
		set(value) {
			if (behaviorSubject.value != value)
				behaviorSubject.onNext(value)
		}

	override fun subscribeActual(observer: Observer<in T>?) {
		behaviorSubject.safeSubscribe(observer)
	}
}
