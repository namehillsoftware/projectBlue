package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise

sealed interface ContinuableResult<Progress>

class HaltedResult<Progress> private constructor() : ContinuableResult<Progress> {

	companion object {
		private val instance by lazy { HaltedResult<Any?>() }

		@Suppress("UNCHECKED_CAST")
		fun <Progress> halted(): HaltedResult<Progress> = instance as HaltedResult<Progress>
	}
}

class ContinuingResult<Progress>(
	val current: Progress,
	val next: Promise<ContinuableResult<Progress>>
) : ContinuableResult<Progress>
