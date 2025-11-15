package com.lasthopesoftware.bluewater.client.browsing.library.settings

sealed interface PasswordStoredConnectionSettings : StoredConnectionSettings {
	val password: String?

	companion object {
		inline fun <reified T : PasswordStoredConnectionSettings> PasswordStoredConnectionSettings.copy(password: String? = this.password) : T = when (this) {
			is StoredSubsonicConnectionSettings -> this.copy(password = password)
			is StoredMediaCenterConnectionSettings -> this.copy(password = password)
		} as T
	}
}

