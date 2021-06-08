package com.lasthopesoftware.bluewater.client.connection.libraries

object ConnectionSettingsValidation : ValidateConnectionSettings {
	override fun isValid(connectionSettings: ConnectionSettings?): Boolean =
		connectionSettings?.accessCode?.isNotEmpty() ?: false
}
