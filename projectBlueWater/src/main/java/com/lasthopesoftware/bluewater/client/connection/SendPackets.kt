package com.lasthopesoftware.bluewater.client.connection

import com.namehillsoftware.handoff.promises.Promise

interface SendPackets {
	fun promiseSentPackets(host: String, port: Int, packets: ByteArray): Promise<Unit>
}
