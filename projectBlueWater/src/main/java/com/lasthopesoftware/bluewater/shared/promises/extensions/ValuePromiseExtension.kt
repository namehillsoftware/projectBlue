package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

fun <T> T.toPromise(): Promise<T> {
	@Suppress("UNCHECKED_CAST")
	return when (this) {
		is Unit -> UnitPromise as Promise<T>
		else -> Promise(this)
	}
}

fun Unit.toPromise(): Promise<Unit> {
	return UnitPromise
}

private object UnitPromise : Promise<Unit>(Unit)
