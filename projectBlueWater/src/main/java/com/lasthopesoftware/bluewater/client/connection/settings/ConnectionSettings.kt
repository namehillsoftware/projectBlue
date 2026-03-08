package com.lasthopesoftware.bluewater.client.connection.settings

sealed interface ConnectionSettings {
	val customHeaders: Map<String, String>
}
