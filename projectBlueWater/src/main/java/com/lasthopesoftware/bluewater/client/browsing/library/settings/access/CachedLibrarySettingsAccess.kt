package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.PermanentPromiseFunctionCache
import com.lasthopesoftware.promises.ForwardedResponse.Companion.thenForward
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class CachedLibrarySettingsAccess(
	private val libraryProvider: ProvideLibrarySettings,
	private val libraryStorage: StoreLibrarySettings,
	private val cache: CachePromiseFunctions<LibraryId, LibrarySettings?> = PermanentPromiseFunctionCache(),
) : ProvideLibrarySettings, StoreLibrarySettings {
	override fun promiseAllLibrarySettings(): Promise<Collection<LibrarySettings>> =
		libraryProvider
			.promiseAllLibrarySettings()
			.cancelBackThen { libraries, _ ->
				for (library in libraries) {
					library.libraryId?.also { cache.overrideCachedValue(it, library) }
				}
				libraries
			}

	override fun promiseLibrarySettings(libraryId: LibraryId): Promise<LibrarySettings?> =
		cache.getOrAdd(libraryId) { libraryProvider.promiseLibrarySettings(libraryId) }

	override fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings> {
		return librarySettings
			.libraryId
			?.let { cache.getOrAdd(it) { libraryStorage.promiseSavedLibrarySettings(librarySettings).thenForward() } }
			?.eventually { savedSettings ->
				savedSettings
					.takeIf { it == librarySettings }
					?.toPromise()
					?: libraryStorage
						.promiseSavedLibrarySettings(librarySettings)
						.cancelBackThen { settings, _ -> settings.also { cache.overrideCachedValue(librarySettings.libraryId, settings) } }
			}
			?: libraryStorage.promiseSavedLibrarySettings(librarySettings)
	}
}
