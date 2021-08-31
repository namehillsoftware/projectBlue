package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	is Unit -> UnitPromise as Promise<T>
	null -> Promise.empty()
	else -> Promise(this)
}

private object UnitPromise : Promise<Unit>(Unit)

fun <T> Promise<T>?.keepPromise(): Promise<T> = this ?: Promise.empty()

fun <T> Promise<T>?.keepPromise(default: T): Promise<T> = this ?: default.toPromise()

fun <T> Promise<T>.unitResponse(): Promise<Unit> = this.then(UnitResponse.respond())

private class UnitResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit> {
	override fun respond(resolution: Resolution) {}

	companion object {
		private val singleUnitResponse = lazy { UnitResponse<Any>() }

		@Suppress("UNCHECKED_CAST")
		fun <Resolution> respond(): UnitResponse<Resolution> = singleUnitResponse.value as UnitResponse<Resolution>
	}
}
