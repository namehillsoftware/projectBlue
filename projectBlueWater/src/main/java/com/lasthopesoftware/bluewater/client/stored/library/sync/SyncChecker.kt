package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.namehillsoftware.handoff.promises.Promise

class SyncChecker(private val libraryProvider: ILibraryProvider, private val serviceFilesForSync: CollectServiceFilesForSync) : CheckForSync {
	override fun promiseIsSyncNeeded(): Promise<Boolean> {
		return libraryProvider.allLibraries
			.eventually { libraries ->
				Promise.whenAll(libraries.map { l ->
					serviceFilesForSync
						.promiseServiceFilesToSync(l.libraryId)
						.then { sf -> sf.isNotEmpty() }
				})
			}
			.then { emptyStatuses -> emptyStatuses.any { it } }
	}
}
