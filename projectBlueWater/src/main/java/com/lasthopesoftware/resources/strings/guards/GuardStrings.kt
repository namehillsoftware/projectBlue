package com.lasthopesoftware.resources.strings.guards

import com.namehillsoftware.handoff.promises.Promise

interface GuardStrings {
	fun promiseEncryption(plainText: String): Promise<EncryptedString>
	fun promiseDecryption(encryptedString: EncryptedString): Promise<String>
}
