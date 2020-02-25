package com.lasthopesoftware.bluewater.client.connection

import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

interface SendPackets {
	fun promiseSentPackets(url: URL, packets: ByteArray): Promise<Unit>
}
