package com.lasthopesoftware.bluewater.client.connection.settings.translation

import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.shared.TranslateTypes
import com.lasthopesoftware.resources.strings.GuardStrings
import com.namehillsoftware.handoff.promises.Promise

class StoredSubsonicTranslator(
	private val stringGuard: GuardStrings
) : TranslateTypes<StoredSubsonicConnectionSettings, ConnectionSettings> {
	override fun promiseTranslation(from: StoredSubsonicConnectionSettings): Promise<ConnectionSettings> {
		return Promise.empty()
//		return Promise.Proxy { cs ->
//			with(from) {
//				(initializationVector?.let(stringGuard::promiseDecryption)?.also(cs::doCancel)
//					?: password?.let { stringGuard.promiseEncryption(it).also(cs::doCancel).then { password } })
//					?.then { password ->
//						SubsonicConnectionSettings(
//							url = url ?: "",
//							userName = userName ?: "",
//							password = password ?: "",
//							isWakeOnLanEnabled = isWakeOnLanEnabled,
//							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
//							macAddress = macAddress,
//						) as ConnectionSettings
//					}
//					.keepPromise {
//						SubsonicConnectionSettings(
//							url = url ?: "",
//							userName = userName ?: "",
//							password = password ?: "",
//							isWakeOnLanEnabled = isWakeOnLanEnabled,
//							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
//							macAddress = macAddress,
//						)
//					}
//			}
//		}
	}
}
