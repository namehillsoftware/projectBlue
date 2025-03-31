package com.lasthopesoftware.bluewater.client.connection.settings

fun interface ValidateConnectionSettings {
	fun isValid(connectionSettings: MediaCenterConnectionSettings): Boolean
}
