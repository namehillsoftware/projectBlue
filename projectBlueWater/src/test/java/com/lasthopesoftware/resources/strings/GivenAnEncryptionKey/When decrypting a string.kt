package com.lasthopesoftware.resources.strings.GivenAnEncryptionKey

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.encryption.DefaultEncryptionConfiguration
import com.lasthopesoftware.encryption.LookupEncryptionKey
import com.lasthopesoftware.resources.strings.EncryptedStringGuard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When decrypting a string` {
	private val mut by lazy {
		EncryptedStringGuard(
			object : LookupEncryptionKey {
				override val encryptionKey: String = "TVXcQz0qQPcIbjlo"
			},
			DefaultEncryptionConfiguration,
		)
	}

	private var encryptedByteString: String? = null
	private var decryptedByteString: String? = null

	@BeforeAll
	fun act() {
		val encryptedString = mut.promiseEncryption("u8SqPT3JLMqbZdBc").toExpiringFuture().get()
		encryptedByteString = encryptedString?.protectedString
		decryptedByteString = encryptedString?.let(mut::promiseDecryption)?.toExpiringFuture()?.get()
	}

	@Test
	fun `then the encrypted bytes are correct`() {
		assertThat(encryptedByteString).isNotNull.isNotBlank.isNotEqualTo("u8SqPT3JLMqbZdBc")
	}

	@Test
	fun `then the decrypted bytes are correct`() {
		assertThat(decryptedByteString).isEqualTo("u8SqPT3JLMqbZdBc")
	}
}
