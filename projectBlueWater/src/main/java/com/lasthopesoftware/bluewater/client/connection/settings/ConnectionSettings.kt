package com.lasthopesoftware.bluewater.client.connection.settings

data class ConnectionSettings(
	val accessCode: String,
	val userName: String? = null,
	val password: String? = null,
	val isLocalOnly: Boolean = false,
	val isWakeOnLanEnabled: Boolean = false,
)
