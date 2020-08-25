package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.storage.GetFreeSpace
import com.lasthopesoftware.storage.directories.GetPrivateDirectories
import com.lasthopesoftware.storage.directories.GetPublicDirectories
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class SyncDirectoryLookup(
	private val libraryProvider: ILibraryProvider,
	private val publicDrives: GetPublicDirectories,
	private val privateDrives: GetPrivateDirectories,
	private val freeSpace: GetFreeSpace) : LookupSyncDirectory {

	override fun promiseSyncDirectory(libraryId: LibraryId): Promise<File> {
		return getExternalFilesDirectoriesStream(libraryId)
			.then { files -> files.maxBy { f -> freeSpace.getFreeSpace(f) } }
	}

	private fun getExternalFilesDirectoriesStream(libraryId: LibraryId): Promise<Collection<File>> {
		return libraryProvider.getLibrary(libraryId)
			.eventually<Collection<File>> { library ->
				when (library?.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> publicDrives.promisePublicDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.INTERNAL -> privateDrives.promisePrivateDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.CUSTOM -> {
						val customSyncedFilePath = library.customSyncedFilesPath
						if (customSyncedFilePath != null) Promise<Collection<File>>(listOf(File(customSyncedFilePath)))
						else Promise(emptyList())
					}
					else -> Promise(emptyList())
				}
			}
	}

	private fun Promise<Collection<File>>.promiseDirectoriesWithLibrary(libraryId: LibraryId): Promise<Collection<File>> {
		if (libraryId.id < 0) return this
		val libraryIdString = libraryId.id.toString()
		return this.then { files -> files.map { f -> File(f, libraryIdString) } }
	}
}
