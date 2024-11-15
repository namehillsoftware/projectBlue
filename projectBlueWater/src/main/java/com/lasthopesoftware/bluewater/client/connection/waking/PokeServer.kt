package com.lasthopesoftware.bluewater.client.connection.waking

import com.namehillsoftware.handoff.promises.Promise

interface PokeServer {
	fun promiseWakeSignal(machineAddress: MachineAddress): Promise<Unit>
}
