package com.lasthopesoftware.resources.strings.GivenAnEncryptionKey

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.encryption.LookupEncryptionKey
import com.lasthopesoftware.resources.strings.EncryptedStringGuard
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class `When decrypting a string` {
	private val mut by lazy {
		EncryptedStringGuard(
			mockk(),
			object : LookupEncryptionKey {
				override fun buildKeySpec(algorithm: String): SecretKey =
					SecretKeySpec("TVXcQz0qQPcIbjlo".toByteArray(Charsets.UTF_8), algorithm)
			},
			mockk(),
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
