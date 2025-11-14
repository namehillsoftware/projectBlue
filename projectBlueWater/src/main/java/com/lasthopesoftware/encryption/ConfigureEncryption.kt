package com.lasthopesoftware.encryption

interface ConfigureEncryption {
	val algorithm: String
	val blockMode: String
	val padding: String
}

val ConfigureEncryption.transformation: String
	get() = "$algorithm/$blockMode/$padding"
