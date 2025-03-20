package com.lasthopesoftware.bluewater.client.connection.settings

object ConnectionSettingsValidation : ValidateConnectionSettings {
	override fun isValid(connectionSettings: MediaCenterConnectionSettings): Boolean =
		connectionSettings.accessCode.isNotEmpty()
}
