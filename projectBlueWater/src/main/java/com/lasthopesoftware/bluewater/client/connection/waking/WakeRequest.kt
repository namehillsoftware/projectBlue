package com.lasthopesoftware.bluewater.client.connection.waking

import java.net.URL

data class WakeRequest(val targetUrl: URL, val signal: ByteArray) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as WakeRequest

		if (targetUrl != other.targetUrl) return false
		if (!signal.contentEquals(other.signal)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = targetUrl.hashCode()
		result = 31 * result + signal.contentHashCode()
		return result
	}
}
