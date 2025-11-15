package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredEncryptionSettings
import com.lasthopesoftware.resources.gson

object StoredSettingsAndEncryptionCombiner {
	fun combineStoredSettingsAndEncryption(connectionSettings: StoredConnectionSettings, encryptionSettings: StoredEncryptionSettings): String? {
		val jsonSettings = gson.toJsonTree(connectionSettings).asJsonObject
		val encryptedJson = gson.toJsonTree(encryptionSettings)
		jsonSettings.asMap().putAll(encryptedJson.asJsonObject.asMap())
		return gson.toJson(jsonSettings)
	}
}
