package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
			.then { files -> files.maxBy { f -> f.freeSpace } }
	}

	private fun getExternalFilesDirectoriesStream(libraryId: LibraryId): Promise<Iterable<File>> {
		return libraryProvider.getLibrary(libraryId)
			.eventually { library ->
				when (library.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> publicDrives.promisePublicDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.INTERNAL -> privateDrives.promisePrivateDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.CUSTOM -> Promise(sequenceOf(File(library.customSyncedFilesPath)).asIterable())
					else -> Promise(emptyArray<File>().asIterable())
				}
			}
	}

	private fun Promise<Collection<File>>.promiseDirectoriesWithLibrary(libraryId: LibraryId): Promise<Iterable<File>> {
		val promisedStream = this.then { it.asIterable() }
		if (libraryId.id < 0) return promisedStream
		val libraryIdString = libraryId.id.toString()
		return promisedStream.then { files -> files.map { f -> File(f, libraryIdString) } }
	}
}
