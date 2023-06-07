package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface CheckIfAnyStoredItemsExist {
    fun promiseIsAnyStoredItemsOrFiles(libraryId: LibraryId): Promise<Boolean>
}
