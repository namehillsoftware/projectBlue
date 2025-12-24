package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise

interface PromisingLatch : Gate {
	fun reset(): Promise<Boolean>
	fun wait(): Promise<Boolean>
}
