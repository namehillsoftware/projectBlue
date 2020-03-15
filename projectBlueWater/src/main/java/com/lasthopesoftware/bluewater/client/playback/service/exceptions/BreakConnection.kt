package com.lasthopesoftware.bluewater.client.playback.service.exceptions

interface BreakConnection {
	fun isConnectionPastThreshold(): Boolean
}
