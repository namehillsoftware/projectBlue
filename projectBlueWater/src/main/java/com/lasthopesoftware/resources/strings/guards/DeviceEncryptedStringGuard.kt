package com.lasthopesoftware.resources.strings.guards

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.lasthopesoftware.encryption.ConfigureEncryption
import com.lasthopesoftware.encryption.LookupEncryptionKey
import com.lasthopesoftware.encryption.transformation
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.security.auth.x500.X500Principal

// Credit to https://proandroiddev.com/shedding-light-on-android-encryption-android-crypto-api-part-2-cipher-147ff4411e1d#54f2
class DeviceEncryptedStringGuard(
	private val context: Context,
	private val encryptionKeyLookup: LookupEncryptionKey,
	private val configureEncryption: ConfigureEncryption,
) : GuardStrings {

	companion object {
		private const val provider = "AndroidKeyStore"
	}

	private val keyStore by lazy {
		KeyStore.getInstance(provider).apply {
			load(null) // With load function we initialize our keystore
		}
	}

	override fun promiseEncryption(plainText: String): Promise<EncryptedString> = getOrCreateSecretKey()
		.eventually { key ->
			ThreadPools.compute.preparePromise {
				val cipher = Cipher.getInstance(configureEncryption.transformation)
				cipher.init(Cipher.ENCRYPT_MODE, key)
				val cipherBytes = cipher.doFinal(plainText.toByteArray())
				val ivBytes = cipher.iv
				EncryptedString(
					ivBytes.toHexString(),
					cipherBytes.toHexString(),
					configureEncryption.algorithm,
					configureEncryption.blockMode,
					configureEncryption.padding,
				)
			}
		}

	override fun promiseDecryption(encryptedString: EncryptedString): Promise<String> = getOrCreateSecretKey()
		.eventually { key ->
			ThreadPools.compute.preparePromise {
				val cipher = Cipher.getInstance(encryptedString.transformation)
				val encryptedBytes = encryptedString.protectedString.hexToByteArray()
				val ivBytes = encryptedString.initializationVector.hexToByteArray()

				cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ivBytes))

				val plainTextBytes = cipher.doFinal(encryptedBytes)
				plainTextBytes.toString(Charsets.UTF_8)
			}
		}

	private fun getOrCreateSecretKey(): Promise<SecretKey> {
		// Use the encryption key as the alias instead to provide some security
		return encryptionKeyLookup.promiseEncryptionKey().eventually { alias ->
			ThreadPools.compute.preparePromise {
				val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
				val secretKey = existingKey?.secretKey
				if (secretKey != null) return@preparePromise secretKey

				val kpg = KeyPairGenerator.getInstance(configureEncryption.algorithm, provider)

				val spec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					KeyGenParameterSpec
						.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
						.setBlockModes(configureEncryption.blockMode)
						.setEncryptionPaddings(configureEncryption.padding)
						.setUserAuthenticationRequired(false)
						.setRandomizedEncryptionRequired(true)
						.build()
				} else {
					KeyPairGeneratorSpec
						.Builder(context)
						.setAlias(alias)
						.setSubject(X500Principal("CN=$alias"))
						.setKeyType(configureEncryption.algorithm)
						.build()
				}

				kpg.initialize(spec)

				KeyGenerator.getInstance(configureEncryption.algorithm).apply {
					init(spec)
				}.generateKey()
			}
		}
	}
}
