package com.lasthopesoftware.bluewater.client.connection.waking

import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface PokeServer {
	fun promiseWakeSignal(machineAddress: MachineAddress, timesToSendSignal: Int, durationBetween: Duration): Promise<Unit>
}
