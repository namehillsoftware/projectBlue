package com.lasthopesoftware.encryption

import com.lasthopesoftware.bluewater.BuildConfig

object AppEncryptionKeyLookup : LookupEncryptionKey {
	override val encryptionKey = BuildConfig.ENCRYPTION_KEY
}
