package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface UpdateStoredFiles {
    fun promiseStoredFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?>
}
