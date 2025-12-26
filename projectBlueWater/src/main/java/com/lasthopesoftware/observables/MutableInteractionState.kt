package com.lasthopesoftware.observables

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

	/**
     * Atomically sets the value to [update] if the current value is [expect],
     * and returns true if the update was successful.
     *
     * Note: This operation may fail spuriously and does not provide ordering guarantees,
     * so it should not be used for synchronization primitives.
     *
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that the actual value
     * was not equal to the expected value.
     */
    fun compareAndSet(expect: T, update: T): Boolean {
		if (expect == update) return false

		while (computeValue() == expect) {
			value = update
			return true
		}

		return false
	}

	private fun computeValue() = behaviorSubject.value?.value ?: initialValue
}
