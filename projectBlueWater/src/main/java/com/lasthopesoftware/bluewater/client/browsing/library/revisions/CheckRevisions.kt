package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface CheckRevisions {
	fun promiseRevision(libraryId: LibraryId): Promise<Int>
}
