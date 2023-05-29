package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class StoredItemsChecker(
    private val storedItemAccess: AccessStoredItems,
    private val checkForAnyStoredFiles: CheckForAnyStoredFiles
) : CheckIfAnyStoredItemsExist {
    override fun promiseIsAnyStoredItemsOrFiles(libraryId: LibraryId): Promise<Boolean> =
		storedItemAccess.promiseStoredItems(libraryId)
			.eventually { items ->
				if (!items.isEmpty()) true.toPromise() else checkForAnyStoredFiles.promiseIsAnyStoredFiles(libraryId)
			}
}
