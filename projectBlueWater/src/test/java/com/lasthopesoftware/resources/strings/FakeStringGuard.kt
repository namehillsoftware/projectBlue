package com.lasthopesoftware.resources.strings

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FakeStringGuard(guardedStrings: Map<String, EncryptedString> = emptyMap()) : GuardStrings {

	private val guardedStrings = guardedStrings.toMutableMap()
	private val unguardedStrings = guardedStrings.map { (key, value) -> value to key }.toMap().toMutableMap()

	override fun promiseEncryption(plainText: String): Promise<EncryptedString> {
		val guardedString = guardedStrings.getOrPut(plainText) {
			EncryptedString(
				"",
				plainText,
				"",
				"",
				"",
			)
		}

		unguardedStrings[guardedString] = plainText

		return guardedString.toPromise()
	}

	override fun promiseDecryption(encryptedString: EncryptedString): Promise<String> {
		val plainText = unguardedStrings.getOrPut(encryptedString) { encryptedString.protectedString }
		guardedStrings[plainText] = encryptedString
		return plainText.toPromise()
	}
}
