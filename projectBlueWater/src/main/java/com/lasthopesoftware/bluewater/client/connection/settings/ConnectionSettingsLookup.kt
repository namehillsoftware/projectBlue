package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.bluewater.shared.TranslateTypes
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsLookup(
	private val librarySettings: ProvideLibrarySettings,
	private val mediaCenterTranslator: TranslateTypes<StoredMediaCenterConnectionSettings, ConnectionSettings>,
	private val subsonicTranslator: TranslateTypes<StoredSubsonicConnectionSettings, ConnectionSettings>,
) : LookupConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		librarySettings
			.promiseLibrarySettings(libraryId)
			.eventually { it ->
				it?.connectionSettings?.let {
					when (it) {
						is StoredMediaCenterConnectionSettings -> mediaCenterTranslator.promiseTranslation(it)
						is StoredSubsonicConnectionSettings -> subsonicTranslator.promiseTranslation(it)
					}
				}.keepPromise()
			}
}
