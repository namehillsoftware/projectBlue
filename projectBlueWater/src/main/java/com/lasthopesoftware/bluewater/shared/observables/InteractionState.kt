package com.lasthopesoftware.bluewater.shared.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.rxjava3.core.Observable

abstract class InteractionState<T> : Observable<NullBox<T>>() {
	abstract val value: T
}
