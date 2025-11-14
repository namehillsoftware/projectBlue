package com.lasthopesoftware.bluewater.client.browsing.library.settings

import com.lasthopesoftware.encryption.EncryptionConfiguration

sealed interface PasswordStoredConnectionSettings : StoredConnectionSettings {
	val password: String?
	val initializationVector: String?
	val encryptionConfiguration: EncryptionConfiguration?
}

