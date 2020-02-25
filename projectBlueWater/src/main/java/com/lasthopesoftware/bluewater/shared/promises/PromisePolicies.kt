package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise

object PromisePolicies {
	fun <Resolution>repeat(function: () -> Promise<Resolution>, maximumRepeats: Int) : Promise<Resolution> {
		return RepeatMachine(function, maximumRepeats).repeat()
	}

	private class RepeatMachine<Resolution>(private val function: () -> Promise<Resolution>, private val maximumRepeats: Int) {
		private var repeatCount = 0

		fun repeat(): Promise<Resolution> {
			return function()
				.eventually {
					if (++repeatCount > maximumRepeats) repeat()
					else Promise(it)
				}
		}
	}
}
