package com.lasthopesoftware.bluewater.client.connection.waking

import com.namehillsoftware.handoff.promises.Promise

class ServerAlarm : WakeServer {
	override fun promiseWakeRequest(machine: Machine): Promise<Boolean> {
		return Promise(true)
	}
}
