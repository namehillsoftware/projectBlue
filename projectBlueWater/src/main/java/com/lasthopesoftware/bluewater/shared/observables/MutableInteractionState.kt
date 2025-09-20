package com.lasthopesoftware.bluewater.shared.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.BehaviorSubject

class MutableInteractionState<T>(private val initialValue: T) : InteractionState<T>() {

	private val behaviorSubject = BehaviorSubject.createDefault(NullBox(initialValue))

	override var value: T
		get() = computeValue()
		set(newValue) {
			if (computeValue() != newValue)
				behaviorSubject.onNext(NullBox(newValue))
		}

	override fun subscribeActual(observer: Observer<in NullBox<T>>) {
		behaviorSubject.safeSubscribe(observer)
	}

	fun asInteractionState(): InteractionState<T> = this

	private fun computeValue() = behaviorSubject.value?.value ?: initialValue
}
