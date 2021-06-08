package com.lasthopesoftware.bluewater.client.connection.libraries

interface ValidateConnectionSettings {
	fun isValid(connectionSettings: ConnectionSettings?): Boolean
}
