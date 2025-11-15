package com.lasthopesoftware.resources.strings

import com.lasthopesoftware.encryption.ConfigureEncryption
import com.lasthopesoftware.encryption.LookupEncryptionKey
import com.lasthopesoftware.encryption.transformation
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Credit to https://proandroiddev.com/shedding-light-on-android-encryption-android-crypto-api-part-2-cipher-147ff4411e1d#54f2
class ApplicationEncryptedStringGuard(
	private val encryptionKeyLookup: LookupEncryptionKey,
	private val configureEncryption: ConfigureEncryption,
) : GuardStrings {
	override fun promiseEncryption(plainText: String): Promise<EncryptedString> = ThreadPools.compute.preparePromise {
		val keySpec = getKeySpec()

		val cipher = Cipher.getInstance(configureEncryption.transformation)
		val ivBytes = generateRandomIv(cipher.blockSize)
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(ivBytes))
		val cipherBytes = cipher.doFinal(plainText.toByteArray())
		EncryptedString(
			ivBytes.toHexString(),
			cipherBytes.toHexString(),
			configureEncryption.algorithm,
			configureEncryption.blockMode,
			configureEncryption.padding,
		)
	}

	override fun promiseDecryption(encryptedString: EncryptedString): Promise<String> = ThreadPools.compute.preparePromise {
		val cipher = Cipher.getInstance(encryptedString.transformation)
		val encryptedBytes = encryptedString.protectedString.hexToByteArray()
		val ivBytes = encryptedString.initializationVector.hexToByteArray()

		cipher.init(Cipher.DECRYPT_MODE, getKeySpec(), IvParameterSpec(ivBytes))

		val plainTextBytes = cipher.doFinal(encryptedBytes)
		plainTextBytes.toString(Charsets.UTF_8)
	}

	private fun getKeySpec(): SecretKey {
		return SecretKeySpec(encryptionKeyLookup.encryptionKey.toByteArray(), configureEncryption.algorithm)
	}

	private fun generateRandomIv(size: Int): ByteArray {
		val random = SecureRandom()
		val iv = ByteArray(size)
		random.nextBytes(iv)
		return iv
	}
}
