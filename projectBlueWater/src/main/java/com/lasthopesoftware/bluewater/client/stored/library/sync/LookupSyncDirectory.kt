package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface LookupSyncDirectory {
	fun promiseSyncDirectory(libraryId: LibraryId): Promise<File>
}
