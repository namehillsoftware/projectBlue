package com.lasthopesoftware.encryption

object DefaultEncryptionConfiguration : ConfigureEncryption {
	override val algorithm: String = "AES"
	override val blockMode: String = "CBC"
	override val padding: String = "PKCS5Padding"
}
