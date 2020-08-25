package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface CollectServiceFilesForSync {
	fun promiseServiceFilesToSync(libraryId: LibraryId): Promise<Collection<ServiceFile>>
}
