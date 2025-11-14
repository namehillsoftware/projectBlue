package com.lasthopesoftware.encryption

data class EncryptionConfiguration(
	override val algorithm: String,
	override val blockMode: String,
	override val padding: String,
) : ConfigureEncryption {
	constructor(from: ConfigureEncryption):
		this(algorithm = from.algorithm, blockMode = from.blockMode, padding = from.padding)
}
