package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise

object UnkeptPromise {
	private class Unkept<Resolution> : Promise<Resolution>()

	private val anyInstance by lazy { Unkept<Any?>() }

	@Suppress("UNCHECKED_CAST")
	@JvmStatic
	fun <Resolution> instance(): Promise<Resolution> = anyInstance as Promise<Resolution>
}
