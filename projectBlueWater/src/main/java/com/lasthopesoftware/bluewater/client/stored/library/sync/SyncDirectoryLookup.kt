package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.storage.GetFreeSpace
import com.lasthopesoftware.storage.directories.GetPrivateDirectories
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class SyncDirectoryLookup(
	private val librarySettingsProvider: ProvideLibrarySettings,
	private val privateDrives: GetPrivateDirectories,
	private val freeSpace: GetFreeSpace,
) : LookupSyncDirectory {

	companion object {
		private val lazyEmptyFiles by lazy { Promise<Collection<File>>(emptyList()) }
	}

	override fun promiseSyncDirectory(libraryId: LibraryId): Promise<File?> =
		getExternalFilesDirectories(libraryId)
			.then { files -> files.maxByOrNull(freeSpace::getFreeSpace) }

	private fun getExternalFilesDirectories(libraryId: LibraryId): Promise<Collection<File>> =
		librarySettingsProvider
			.promiseLibrarySettings(libraryId)
			.eventually { settings ->
				settings
					?.takeIf { it.syncedFileLocation == SyncedFileLocation.INTERNAL }
					?.run {
						val promisedPrivateDrive = privateDrives.promisePrivateDrives()
						if (settings.libraryId == null) promisedPrivateDrive
						else promisedPrivateDrive.then { files -> files.map { f -> File(f, libraryId.id.toString()) } }
					}
					?: lazyEmptyFiles
			}
}
