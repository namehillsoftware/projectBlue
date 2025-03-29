package com.lasthopesoftware.resources.strings

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Base64Encoder : EncodeToBase64 {
	private val base64 = Base64.Default

	override fun encodeString(decodedString: String): String = encodeBytes(decodedString.toByteArray())

	override fun encodeBytes(bytes: ByteArray): String = base64.encode(bytes)

	override fun decodeToBytes(string: String): ByteArray = base64.decode(string)
}
