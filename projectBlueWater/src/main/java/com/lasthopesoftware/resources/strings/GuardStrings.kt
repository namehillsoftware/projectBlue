package com.lasthopesoftware.resources.strings

import com.namehillsoftware.handoff.promises.Promise

interface GuardStrings {
	fun promiseEncryption(plainText: String): Promise<EncryptedString>
	fun promiseDecryption(encryptedString: EncryptedString): Promise<String>
}
