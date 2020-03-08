package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.resources.executors.HttpThreadPoolExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class PacketSender : SendPackets {
	override fun promiseSentPackets(host: String, port: Int, packets: ByteArray): Promise<Unit> {
		return QueuedPromise(MessageWriter {
			val address = InetAddress.getByName(host)
			val packet = DatagramPacket(packets, packets.size, address, port)
			DatagramSocket().use { it.send(packet) }
		}, HttpThreadPoolExecutor.executor)
	}
}
