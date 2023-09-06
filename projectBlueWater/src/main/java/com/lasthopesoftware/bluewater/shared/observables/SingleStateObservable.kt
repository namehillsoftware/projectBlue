package com.lasthopesoftware.bluewater.shared.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.Observer

class SingleStateObservable<T : Any>(override val value: T) : ReadOnlyStateObservable<T>() {

	override fun subscribeActual(observer: Observer<in NullBox<T>>?) {
		just(NullBox(value)).safeSubscribe(observer)
	}
}
