package com.lasthopesoftware.resources.strings.guards

import com.lasthopesoftware.encryption.ConfigureEncryption
import com.lasthopesoftware.encryption.LookupEncryptionKey
import com.lasthopesoftware.encryption.transformation
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Credit to https://proandroiddev.com/shedding-light-on-android-encryption-android-crypto-api-part-2-cipher-147ff4411e1d#54f2
class ApplicationEncryptedStringGuard(
	private val encryptionKeyLookup: LookupEncryptionKey,
	private val configureEncryption: ConfigureEncryption,
) : GuardStrings {
	private val keySpec by RetryOnRejectionLazyPromise {
		encryptionKeyLookup
			.promiseEncryptionKey()
			.then { k ->
				SecretKeySpec(k.toByteArray(), configureEncryption.algorithm)
			}
	}

	override fun promiseEncryption(plainText: String): Promise<EncryptedString> = keySpec.eventually { spec ->
		ThreadPools.compute.preparePromise {
			val cipher = Cipher.getInstance(configureEncryption.transformation)
			val ivBytes = generateRandomIv(cipher.blockSize)
			cipher.init(Cipher.ENCRYPT_MODE, spec, IvParameterSpec(ivBytes))
			val cipherBytes = cipher.doFinal(plainText.toByteArray())
			EncryptedString(
				ivBytes.toHexString(),
				cipherBytes.toHexString(),
				configureEncryption.algorithm,
				configureEncryption.blockMode,
				configureEncryption.padding,
			)
		}
	}

	override fun promiseDecryption(encryptedString: EncryptedString): Promise<String> = keySpec.eventually { spec ->
		ThreadPools.compute.preparePromise {
			val cipher = Cipher.getInstance(encryptedString.transformation)
			val encryptedBytes = encryptedString.protectedString.hexToByteArray()
			val ivBytes = encryptedString.initializationVector.hexToByteArray()

			cipher.init(Cipher.DECRYPT_MODE, spec, IvParameterSpec(ivBytes))

			val plainTextBytes = cipher.doFinal(encryptedBytes)
			plainTextBytes.toString(Charsets.UTF_8)
		}
	}

	private fun generateRandomIv(size: Int): ByteArray {
		val random = SecureRandom()
		val iv = ByteArray(size)
		random.nextBytes(iv)
		return iv
	}
}
