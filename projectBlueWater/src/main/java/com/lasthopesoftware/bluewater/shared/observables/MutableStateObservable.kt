package com.lasthopesoftware.bluewater.shared.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.Observer
import io.reactivex.subjects.BehaviorSubject

class MutableStateObservable<T>(private val initialValue: T) : ReadOnlyStateObservable<T>() {

	private val behaviorSubject = BehaviorSubject.createDefault(NullBox(initialValue))

	override var value: T
		get() = behaviorSubject.value?.value ?: initialValue
		set(value) {
			if (behaviorSubject.value != value)
				behaviorSubject.onNext(NullBox(value))
		}

	override fun subscribeActual(observer: Observer<in NullBox<T>>?) {
		behaviorSubject.safeSubscribe(observer)
	}
}
