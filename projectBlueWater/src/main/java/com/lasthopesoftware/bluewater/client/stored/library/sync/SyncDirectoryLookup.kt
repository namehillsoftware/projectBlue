package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.library.repository.Library
import com.lasthopesoftware.bluewater.client.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.storage.directories.GetPrivateDirectories
import com.lasthopesoftware.storage.directories.GetPublicDirectories
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class SyncDirectoryLookup(
	private val libraryProvider: ILibraryProvider,
	private val publicDrives: GetPublicDirectories,
	private val privateDrives: GetPrivateDirectories) : LookupSyncDirectory {

	override fun promiseSyncDirectory(libraryId: LibraryId): Promise<File> {
		return getExternalFilesDirectoriesStream(libraryId)
			.then { files -> files.maxBy { it.freeSpace } }
	}

	private fun getExternalFilesDirectoriesStream(libraryId: LibraryId): Promise<List<File>> {
		return libraryProvider.getLibrary(libraryId)
			.eventually { library: Library ->
				when (library.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> publicDrives.promisePublicDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.INTERNAL -> privateDrives.promisePrivateDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.CUSTOM -> Promise(listOf(File(library.customSyncedFilesPath)))
					else -> Promise(emptyList())
				}
			}
	}

	private fun Promise<List<File>>.promiseDirectoriesWithLibrary(libraryId: LibraryId): Promise<List<File>> {
		if (libraryId.id < 0) return this
		val libraryIdString = libraryId.id.toString()
		return this.then { files -> files.map { f -> File(f, libraryIdString) } }
	}

}
