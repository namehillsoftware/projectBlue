package com.lasthopesoftware.encryption

import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

object AppEncryptionKeyLookup : LookupEncryptionKey {
	private val cachedEncryptionKey by lazy { BuildConfig.ENCRYPTION_KEY.toPromise() }

	override fun promiseEncryptionKey(): Promise<String> = cachedEncryptionKey
}
