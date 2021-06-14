package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.resources.strings.EncodeToBase64

object PassThroughBase64Encoder : EncodeToBase64 {
	override fun encodeString(decodedString: String): String = decodedString
}
