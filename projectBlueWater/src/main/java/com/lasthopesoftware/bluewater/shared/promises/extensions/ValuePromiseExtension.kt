package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

@Suppress("UNCHECKED_CAST")
fun <T> T.toPromise(): Promise<T> = when (this) {
	is Unit -> UnitPromise as Promise<T>
	else -> Promise(this)
}

fun Unit.toPromise(): Promise<Unit> = UnitPromise

private object UnitPromise : Promise<Unit>(Unit)

fun <T> Promise<T>.unitResponse(): Promise<Unit> = this.then(UnitResponse.respond())

private class UnitResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit> {
	override fun respond(resolution: Resolution) {}

	companion object {
		private val singlePassThrough = lazy { UnitResponse<Any>() }

		@Suppress("UNCHECKED_CAST")
		fun <Resolution> respond(): UnitResponse<Resolution> = singlePassThrough.value as UnitResponse<Resolution>
	}
}
