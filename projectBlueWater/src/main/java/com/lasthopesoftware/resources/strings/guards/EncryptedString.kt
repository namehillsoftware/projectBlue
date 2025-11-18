package com.lasthopesoftware.resources.strings.guards

import com.lasthopesoftware.encryption.ConfigureEncryption

data class EncryptedString(
	val initializationVector: String,
	val protectedString: String,
	override val algorithm: String,
	override val blockMode: String,
	override val padding: String,
) : ConfigureEncryption
