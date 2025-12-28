package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise

interface Gate {
	fun open(): Gate
	fun reset(): Promise<Boolean>
}
