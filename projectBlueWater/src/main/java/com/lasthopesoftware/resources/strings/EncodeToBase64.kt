package com.lasthopesoftware.resources.strings

interface EncodeToBase64 {
	fun encodeString(decodedString: String): String
	fun encodeBytes(bytes: ByteArray): String
	fun decodeToBytes(string: String): ByteArray
}
