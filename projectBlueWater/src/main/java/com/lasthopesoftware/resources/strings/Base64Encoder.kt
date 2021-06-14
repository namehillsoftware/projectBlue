package com.lasthopesoftware.resources.strings

import android.util.Base64

class Base64Encoder : EncodeToBase64 {
	override fun encodeString(decodedString: String): String {
		return Base64.encodeToString(decodedString.toByteArray(), Base64.DEFAULT).trim { it <= ' ' }
	}
}
