package com.lasthopesoftware.bluewater.shared.observables

import io.reactivex.Observer

class SingleStateObservable<T : Any>(override val value: T) : ReadOnlyStateObservable<T>() {

	override fun subscribeActual(observer: Observer<in T>?) {
		just(value).safeSubscribe(observer)
	}
}
