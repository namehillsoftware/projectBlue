package com.lasthopesoftware.resources.strings

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.lasthopesoftware.encryption.ConfigureEncryption
import com.lasthopesoftware.encryption.LookupEncryptionKey
import com.lasthopesoftware.encryption.transformation
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.security.auth.x500.X500Principal

// Credit to https://proandroiddev.com/shedding-light-on-android-encryption-android-crypto-api-part-2-cipher-147ff4411e1d#54f2
class EncryptedStringGuard(
	private val context: Context,
	private val encryptionKeyLookup: LookupEncryptionKey,
	private val configureEncryption: ConfigureEncryption
) : GuardStrings {
	companion object {
		private const val alias = "projectblue"
		private const val provider = "AndroidKeyStore"
	}

	override fun promiseEncryption(plainText: String): Promise<EncryptedString> = ThreadPools.compute.preparePromise {
//		val spec = KeyPairGeneratorSpec
//			.Builder(context)
//			.setAlias(alias)
//			.setSubject(X500Principal("CN=$alias"))
//			.build()
//
//		val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
//			KeyProperties.KEY_ALGORITHM_EC,
//			provider
//		)
//		kpg.initialize(spec)
//		kpg.generateKeyPair()
//		val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
//			alias,
//			KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
//		).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512).build()
//
//		kpg.initialize(parameterSpec)
//
//		val kp = kpg.generateKeyPair()

//		val key = BuildConfig.ENCRYPTION_KEY
		val keySpec = getKeySpec()
//		PBEKeySpec(
//			plaintext.toCharArray(), "".toByteArray(),
//			iterationCount, keyLength
//		)
//		keygen.init(256)

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

	private fun getKeyPairGenerator(): KeyPairGenerator {
		val kpg = KeyPairGenerator.getInstance(configureEncryption.algorithm, provider)

		val spec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			KeyGenParameterSpec
				.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
				.setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
				.build()
//
//			val spec = KeyGenParameterSpec.Builder(
//				alias,
//				KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
//			).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512).build()
//
//			kpg.initialize(spec)
//
//			kpg
		} else {
			KeyPairGeneratorSpec
				.Builder(context)
				.setAlias(alias)
				.setSubject(X500Principal("CN=$alias"))
				.build()
//			val spec = KeyPairGeneratorSpec
//				.Builder(context)
//				.setAlias(alias)
//				.setSubject(X500Principal("CN=$alias"))
//				.build()
//
//			kpg.initialize(spec)
//
//			kpg
		}

		kpg.initialize(spec)

		return kpg
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private fun getKeyPairGreaterThan23(): KeyPairGenerator {
		val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, provider)

		val parameterSpec = KeyGenParameterSpec
			.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
			.setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
			.build()

		kpg.initialize(parameterSpec)

		return kpg
	}

//	private fun getKeyPairLessThan23(): KeyPairGenerator {
//		val spec = KeyPairGeneratorSpec
//			.Builder(context)
//			.setAlias(alias)
//			.setSubject(X500Principal("CN=$alias"))
//			.build()
//
//		val kpg = KeyPairGenerator.getInstance(
//			KeyProperties.KEY_ALGORITHM_AES,
//			provider
//		)
//
//		kpg.initialize(spec)
//
//		return kpg
//	}

	private fun getKeySpec(): SecretKey {
		return encryptionKeyLookup.buildKeySpec(configureEncryption.algorithm)
	}

	private fun generateRandomIv(size: Int): ByteArray {
		val random = SecureRandom()
		val iv = ByteArray(size)
		random.nextBytes(iv)
		return iv
	}
}
