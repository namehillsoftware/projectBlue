package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface PruneStoredFiles {
	fun pruneDanglingFiles(): Promise<Unit>
	fun pruneStoredFiles(libraryId: LibraryId): Promise<Unit>
}
