package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.ManageLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class LibrarySettingsAccess(private val libraryManager: ManageLibraries) : ProvideLibrarySettings, StoreLibrarySettings {
	companion object {
		private val serverTypeNames by lazy { Library.ServerType.entries.map { it.name } }

		private fun librarySettingsAccessCancelled() = CancellationException("Cancelled while accessing Library Settings.")

		private fun LibrarySettings.toNewLibrary() = Library(
			libraryName = libraryName,
			isUsingExistingFiles = isUsingExistingFiles,
			serverType = Library.ServerType.MediaCenter.name,
			syncedFileLocation = syncedFileLocation,
			connectionSettings = connectionSettings?.let(Json::encodeToString),
		)

		private fun Library.toLibrarySettings() = LibrarySettings(
			libraryId = libraryId,
			isUsingExistingFiles = isUsingExistingFiles,
			libraryName = libraryName,
			syncedFileLocation = syncedFileLocation,
			connectionSettings = connectionSettings
				?.takeIf { serverTypeNames.contains(serverType) }
				?.let { Json.decodeFromString<StoredMediaCenterConnectionSettings>(it) }
		)
	}

	override fun promiseAllLibrarySettings(): Promise<Collection<LibrarySettings>> =
		libraryManager
			.promiseAllLibraries()
			.cancelBackEventually { libraries ->
				Promise.whenAll(
					libraries.map { l ->
						ThreadPools.compute.preparePromise { cs ->
							if (cs.isCancelled) throw librarySettingsAccessCancelled()

							l.toLibrarySettings()
						}
					}
				)
			}

	override fun promiseLibrarySettings(libraryId: LibraryId): Promise<LibrarySettings?> =
		libraryManager
			.promiseLibrary(libraryId)
			.cancelBackEventually { maybeLibrary ->
				maybeLibrary
					?.let {
						ThreadPools.compute.preparePromise { cs ->
							if (cs.isCancelled) throw librarySettingsAccessCancelled()

							it.toLibrarySettings()
						}
					}
					.keepPromise()
			}

	override fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings> =
		(librarySettings.libraryId
			?.let(libraryManager::promiseLibrary)
			?.cancelBackEventually { maybeLibrary ->
				maybeLibrary
					?.let { l ->
						ThreadPools.compute.preparePromise { cs ->
							if (cs.isCancelled) throw librarySettingsAccessCancelled()

							l.libraryName = librarySettings.libraryName
							l.isUsingExistingFiles = librarySettings.isUsingExistingFiles
							l.syncedFileLocation = librarySettings.syncedFileLocation

							l.serverType =
								if (librarySettings.connectionSettings != null) Library.ServerType.MediaCenter.name
								else null

							l.connectionSettings = librarySettings.connectionSettings?.let(Json::encodeToString)
							l
						}
					}
					?: ThreadPools.compute.preparePromise { cs ->
						if (cs.isCancelled) throw librarySettingsAccessCancelled()
						librarySettings.toNewLibrary()
					}
			}
			?: ThreadPools.compute.preparePromise { cs ->
				if (cs.isCancelled) throw librarySettingsAccessCancelled()
				librarySettings.toNewLibrary()
			})
			.cancelBackEventually(libraryManager::saveLibrary)
			.cancelBackEventually {
				ThreadPools.compute.preparePromise { cs ->
					if (cs.isCancelled) throw librarySettingsAccessCancelled()

					it.toLibrarySettings()
				}
			}
}
