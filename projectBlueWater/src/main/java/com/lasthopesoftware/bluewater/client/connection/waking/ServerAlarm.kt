package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ServerAlarm(private val connectionProvider: IConnectionProvider) : WakeServer {
	override fun promiseWakeRequest(machine: Machine): Promise<Unit> {
		val macBytes = getMacBytes(machine.macAddress)
		val bytes = ByteArray(6 + 16 * macBytes.size)
		for (i in 0..5) {
			bytes[i] = 0xff.toByte()
		}

		for (i in 6 until bytes.size step macBytes.size) {
			System.arraycopy(macBytes, 0, bytes, i, macBytes.size);
		}

		return connectionProvider.promiseSentPacket(bytes)
	}

	companion object {
		private fun getMacBytes(macStr: String): ByteArray {
			val hex = macStr.split(":", "-")
			require(hex.size == 6) { "Invalid MAC address." }
			try {
				return hex.map { it.toInt(16).toByte() }.toByteArray()
			} catch (e: NumberFormatException) {
				throw IllegalArgumentException("Invalid hex digit in MAC address.")
			}
		}
	}
}
