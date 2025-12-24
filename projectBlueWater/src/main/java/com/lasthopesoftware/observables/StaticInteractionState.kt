package com.lasthopesoftware.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.rxjava3.core.Observer

class StaticInteractionState<T>(override val value: T) : InteractionState<T>() {
	override fun subscribeActual(observer: Observer<in NullBox<T>>) {
		observer.onNext(NullBox(value))
	}
}
