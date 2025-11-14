package com.lasthopesoftware.encryption

import javax.crypto.SecretKey

interface LookupEncryptionKey {
	fun buildKeySpec(algorithm: String): SecretKey
}
