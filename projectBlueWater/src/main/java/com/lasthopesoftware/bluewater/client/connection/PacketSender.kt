package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class PacketSender : SendPackets {
	override fun promiseSentPackets(host: String, port: Int, packets: ByteArray): Promise<Unit> =
		ThreadPools.io.preparePromise { t ->
			val address = InetAddress.getByName(host)
			val packet = DatagramPacket(packets, packets.size, address, port)
			if (!t.isCancelled)
				DatagramSocket().use { it.send(packet) }
		}
}
