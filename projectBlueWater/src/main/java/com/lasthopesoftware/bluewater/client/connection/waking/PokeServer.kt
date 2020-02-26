package com.lasthopesoftware.bluewater.client.connection.waking

import com.namehillsoftware.handoff.promises.Promise

interface PokeServer {
	fun promiseWakeSignal(machine: Machine): Promise<Unit>
}
