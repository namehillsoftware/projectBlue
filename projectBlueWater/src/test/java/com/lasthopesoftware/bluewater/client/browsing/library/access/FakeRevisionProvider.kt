package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.namehillsoftware.handoff.promises.Promise

class FakeRevisionProvider(private val version: Int) : CheckRevisions {
	override fun promiseRevision(libraryId: LibraryId): Promise<Int> = Promise(version)
}
