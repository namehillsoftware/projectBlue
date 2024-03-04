package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SyncChecker(
	private val libraryProvider: ILibraryProvider,
	private val serviceFilesForSync: CollectServiceFilesForSync,
	private val checkForAnyStoredFiles: CheckForAnyStoredFiles,
) : CheckForSync {
	override fun promiseIsSyncNeeded(): Promise<Boolean> {
		return libraryProvider.allLibraries
			.eventually { libraries ->
				Promise.whenAll(libraries.map { l ->
					serviceFilesForSync
						.promiseServiceFilesToSync(l.libraryId)
						.eventually { sf ->
							if (sf.isNotEmpty()) true.toPromise()
							else checkForAnyStoredFiles.promiseIsAnyStoredFiles(l.libraryId)
						}
				})
			}
			.then { emptyStatuses -> emptyStatuses.any { it } }
	}
}
