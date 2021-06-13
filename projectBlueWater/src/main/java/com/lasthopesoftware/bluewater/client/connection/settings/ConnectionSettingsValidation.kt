package com.lasthopesoftware.bluewater.client.connection.settings

object ConnectionSettingsValidation : ValidateConnectionSettings {
	override fun isValid(connectionSettings: ConnectionSettings): Boolean =
		connectionSettings.accessCode?.isNotEmpty() ?: false
}
