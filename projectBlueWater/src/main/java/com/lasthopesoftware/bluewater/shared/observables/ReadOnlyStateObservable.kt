package com.lasthopesoftware.bluewater.shared.observables

import io.reactivex.Observable

abstract class ReadOnlyStateObservable<T : Any> : Observable<T>() {
	abstract val value: T
}
