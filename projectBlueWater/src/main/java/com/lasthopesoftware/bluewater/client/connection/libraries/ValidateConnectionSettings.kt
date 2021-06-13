package com.lasthopesoftware.bluewater.client.connection.libraries

fun interface ValidateConnectionSettings {
	fun isValid(connectionSettings: ConnectionSettings): Boolean
}
