package com.lasthopesoftware.bluewater.client.browsing.library.settings

import com.lasthopesoftware.encryption.EncryptionConfiguration

data class StoredEncryptionSettings(
	val password: String? = null,
    val initializationVector: String? = null,
    val encryptionConfiguration: EncryptionConfiguration? = null,
)
