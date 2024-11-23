package com.lasthopesoftware.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.BehaviorSubject

class LiftedInteractionState<T: Any>(source: Observable<T>, private val initialValue: T) : InteractionState<T>(), AutoCloseable {

	private val behaviorSubject = BehaviorSubject.createDefault(NullBox(initialValue))
	private val subscription = source.distinctUntilChanged().subscribe { behaviorSubject.onNext(NullBox(it)) }

	override val value: T
		get() = behaviorSubject.value?.value ?: initialValue

	override fun subscribeActual(observer: Observer<in NullBox<T>>) {
		behaviorSubject.safeSubscribe(observer)
	}

	override fun close() {
		subscription.dispose()
	}
}
