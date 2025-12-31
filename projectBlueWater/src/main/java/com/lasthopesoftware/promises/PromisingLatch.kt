package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise

interface PromisingLatch : Gate {
	fun wait(): Promise<Boolean>
}
