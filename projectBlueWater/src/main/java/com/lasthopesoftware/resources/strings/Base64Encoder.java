package com.lasthopesoftware.resources.strings;

import android.util.Base64;

public class Base64Encoder implements EncodeToBase64 {
	@Override
	public String encodeString(String decodedString) {
		return Base64.encodeToString(decodedString.getBytes(), Base64.DEFAULT);
	}
}
