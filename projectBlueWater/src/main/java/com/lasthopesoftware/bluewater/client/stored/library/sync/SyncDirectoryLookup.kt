package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.annimon.stream.Stream
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
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
			.then { files -> files.sortBy { f -> f.freeSpace }.findLast().orElse(null) }
	}

	private fun getExternalFilesDirectoriesStream(libraryId: LibraryId): Promise<Stream<File>> {
		return libraryProvider.getLibrary(libraryId)
			.eventually { library ->
				when (library.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> publicDrives.promisePublicDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.INTERNAL -> privateDrives.promisePrivateDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.CUSTOM -> Promise(Stream.of(File(library.customSyncedFilesPath)))
					else -> Promise(Stream.empty())
				}
			}
	}

	private fun Promise<Collection<File>>.promiseDirectoriesWithLibrary(libraryId: LibraryId): Promise<Stream<File>> {
		val promisedStream = this.then { Stream.of(it) }
		if (libraryId.id < 0) return promisedStream
		val libraryIdString = libraryId.id.toString()
		return promisedStream.then { files -> files.map { f -> File(f, libraryIdString) } }
	}
}
