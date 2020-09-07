package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	is Unit -> UnitPromise as Promise<T>
	else -> Promise(this)
}

fun Unit.toPromise(): Promise<Unit> = UnitPromise

private object UnitPromise : Promise<Unit>(Unit)
