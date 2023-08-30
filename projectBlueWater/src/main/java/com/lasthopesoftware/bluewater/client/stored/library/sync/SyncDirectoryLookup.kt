package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.storage.GetFreeSpace
import com.lasthopesoftware.storage.directories.GetPrivateDirectories
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class SyncDirectoryLookup(
	private val libraryProvider: ILibraryProvider,
	private val privateDrives: GetPrivateDirectories,
	private val freeSpace: GetFreeSpace,
) : LookupSyncDirectory {

	companion object {
		private val lazyEmptyFiles by lazy { Promise<Collection<File>>(emptyList()) }
	}

	override fun promiseSyncDirectory(libraryId: LibraryId): Promise<File?> =
		getExternalFilesDirectoriesStream(libraryId)
			.then { files -> files.maxByOrNull(freeSpace::getFreeSpace) }

	private fun getExternalFilesDirectoriesStream(libraryId: LibraryId): Promise<Collection<File>> =
		libraryProvider
			.promiseLibrary(libraryId)
			.eventually { library ->
				when (library?.syncedFileLocation) {
					SyncedFileLocation.INTERNAL -> privateDrives.promisePrivateDrives().promiseDirectoriesWithLibrary(libraryId)
					else -> lazyEmptyFiles
				}
			}

	private fun Promise<Collection<File>>.promiseDirectoriesWithLibrary(libraryId: LibraryId): Promise<Collection<File>> =
		if (libraryId.id < 0) this
		else then { files -> files.map { f -> File(f, libraryId.id.toString()) } }
}
