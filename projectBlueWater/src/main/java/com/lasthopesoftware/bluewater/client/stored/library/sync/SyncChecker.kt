package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ProvideLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SyncChecker(
    private val libraryProvider: ProvideLibraries,
    private val serviceFilesForSync: CollectServiceFilesForSync,
    private val checkForAnyStoredFiles: CheckForAnyStoredFiles,
) : CheckForSync {
	override fun promiseIsSyncNeeded(): Promise<Boolean> {
		return libraryProvider.promiseAllLibraries()
			.eventually { libraries ->
				Promise.whenAll(libraries.map { l ->
					serviceFilesForSync
						.promiseServiceFilesToSync(l.libraryId)
						.eventually { sf ->
							if (sf.any()) true.toPromise()
							else checkForAnyStoredFiles.promiseIsAnyStoredFiles(l.libraryId)
						}
				})
			}
			.then { emptyStatuses -> emptyStatuses.any { it } }
	}
}
