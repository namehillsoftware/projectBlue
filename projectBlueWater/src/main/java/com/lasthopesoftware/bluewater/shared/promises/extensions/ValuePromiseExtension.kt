package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

fun <T> T.toPromise(): Promise<T> {
	return when (this) {
		is Unit -> UnitPromise as Promise<T>
		null -> Promise.empty()
		else -> Promise(this)
	}
}

private object UnitPromise : Promise<Unit>(Unit)
