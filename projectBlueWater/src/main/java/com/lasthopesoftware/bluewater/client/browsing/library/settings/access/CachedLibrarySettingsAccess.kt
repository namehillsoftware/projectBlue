package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class CachedLibrarySettingsAccess(
	private val libraryProvider: ProvideLibrarySettings,
	private val libraryStorage: StoreLibrarySettings,
) : ProvideLibrarySettings, StoreLibrarySettings {
	private val cachedLibraries = ConcurrentHashMap<LibraryId, LibrarySettings>()

	override fun promiseAllLibrarySettings(): Promise<Collection<LibrarySettings>> =
		libraryProvider
			.promiseAllLibrarySettings()
			.cancelBackThen { libraries, _ ->
				for (library in libraries) {
					library.libraryId?.also { cachedLibraries[it] = library }
				}
				libraries
			}

	override fun promiseLibrarySettings(libraryId: LibraryId): Promise<LibrarySettings?> =
		cachedLibraries[libraryId]?.toPromise()
			?: libraryProvider
			.promiseLibrarySettings(libraryId)
			.cancelBackThen { maybeLibrary, _ ->
				maybeLibrary
					?.also {
						cachedLibraries[libraryId] = it
					}
			}

	override fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings> {
		val libraryId = librarySettings.libraryId
		if (libraryId != null) {
			val cachedSettings = cachedLibraries[libraryId]
			if (cachedSettings != null) {
				if (cachedSettings == librarySettings) {
					return cachedSettings.toPromise()
				}

				cachedLibraries.remove(libraryId, cachedSettings)
			}
		}

		return libraryStorage
				.promiseSavedLibrarySettings(librarySettings)
				.cancelBackThen { updated, _ ->
					updated.libraryId?.also { cachedLibraries[it] = updated }
					updated
				}
	}
}
