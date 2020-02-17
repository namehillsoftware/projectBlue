package com.lasthopesoftware.bluewater.client.connection.waking

import com.namehillsoftware.handoff.promises.Promise

interface WakeServer {
	fun promiseWakeRequest(machine: Machine): Promise<Boolean>
}
