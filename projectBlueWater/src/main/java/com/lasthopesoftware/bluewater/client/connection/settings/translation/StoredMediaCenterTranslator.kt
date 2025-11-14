package com.lasthopesoftware.bluewater.client.connection.settings.translation

import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.shared.TranslateTypes
import com.lasthopesoftware.resources.strings.GuardStrings
import com.namehillsoftware.handoff.promises.Promise

class StoredMediaCenterTranslator(
	private val stringGuard: GuardStrings
) : TranslateTypes<StoredMediaCenterConnectionSettings, ConnectionSettings> {
	override fun promiseTranslation(from: StoredMediaCenterConnectionSettings): Promise<ConnectionSettings> {
		return Promise.empty<ConnectionSettings>()
//		return Promise.Proxy { cs ->
//			with(from) {
//				(initializationVector?.let(stringGuard::promiseDecryption)?.also(cs::doCancel)
//					?: password?.let { stringGuard.promiseEncryption(it).also(cs::doCancel).then { password } })
//					?.then { password ->
//						MediaCenterConnectionSettings(
//							accessCode = accessCode ?: "",
//							userName = userName,
//							password = password,
//							isLocalOnly = isLocalOnly,
//							isWakeOnLanEnabled = isWakeOnLanEnabled,
//							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
//							macAddress = macAddress
//						) as ConnectionSettings
//					}
//					.keepPromise {
//						MediaCenterConnectionSettings(
//							accessCode = accessCode ?: "",
//							userName = userName,
//							password = password,
//							isLocalOnly = isLocalOnly,
//							isWakeOnLanEnabled = isWakeOnLanEnabled,
//							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
//							macAddress = macAddress
//						)
//					}
//			}
//		}
	}
}
