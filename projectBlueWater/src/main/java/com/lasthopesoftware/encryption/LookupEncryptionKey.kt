package com.lasthopesoftware.encryption

import com.namehillsoftware.handoff.promises.Promise

interface LookupEncryptionKey {
	fun promiseEncryptionKey(): Promise<String>
}
