package com.lasthopesoftware.encryption

import com.lasthopesoftware.bluewater.BuildConfig
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object AppEncryptionKeyLookup : LookupEncryptionKey {
	private val encryptionKey = BuildConfig.ENCRYPTION_KEY
	override fun buildKeySpec(algorithm: String): SecretKey =
		SecretKeySpec(encryptionKey.toByteArray(), algorithm)
}
