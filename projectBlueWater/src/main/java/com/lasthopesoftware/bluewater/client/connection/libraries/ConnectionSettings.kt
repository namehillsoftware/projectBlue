package com.lasthopesoftware.bluewater.client.connection.libraries

data class ConnectionSettings(
	val accessCode: String?,
	val userName: String?,
	val password: String?,
	val isLocalOnly: Boolean,
	val isWakeOnLanEnabled: Boolean)
