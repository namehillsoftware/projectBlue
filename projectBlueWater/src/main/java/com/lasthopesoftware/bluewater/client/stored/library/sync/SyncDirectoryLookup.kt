package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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

	companion object {
		private val lazyEmptyFiles = lazy { Promise<Collection<File>>(emptyList()) }
	}

	override fun promiseSyncDirectory(libraryId: LibraryId): Promise<File?> =
		getExternalFilesDirectoriesStream(libraryId)
			.then { files -> files.maxByOrNull(freeSpace::getFreeSpace) }

	private fun getExternalFilesDirectoriesStream(libraryId: LibraryId): Promise<Collection<File>> =
		libraryProvider.getLibrary(libraryId)
			.eventually { library ->
				when (library?.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> publicDrives.promisePublicDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.INTERNAL -> privateDrives.promisePrivateDrives().promiseDirectoriesWithLibrary(libraryId)
					SyncedFileLocation.CUSTOM ->
						library.customSyncedFilesPath
							?.let { p -> listOf(File(p)) }
							?.toPromise()
							?: lazyEmptyFiles.value
					else -> lazyEmptyFiles.value
				}
			}

	private fun Promise<Collection<File>>.promiseDirectoriesWithLibrary(libraryId: LibraryId): Promise<Collection<File>> {
		if (libraryId.id < 0) return this
		val libraryIdString = libraryId.id.toString()
		return this.then { files -> files.map { f -> File(f, libraryIdString) } }
	}
}
