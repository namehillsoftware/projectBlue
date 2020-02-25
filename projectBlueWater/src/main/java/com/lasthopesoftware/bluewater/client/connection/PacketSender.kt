package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.resources.executors.HttpThreadPoolExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL

class PacketSender : SendPackets {
	override fun promiseSentPackets(url: URL, packets: ByteArray): Promise<Unit> {
		return QueuedPromise(MessageWriter {
			val address = InetAddress.getByName(url.host)
			val packet = DatagramPacket(packets, packets.size, address, port)
			DatagramSocket().use { it.send(packet) }
		}, HttpThreadPoolExecutor.executor)
	}

	companion object {
		private const val port = 9
	}
}
