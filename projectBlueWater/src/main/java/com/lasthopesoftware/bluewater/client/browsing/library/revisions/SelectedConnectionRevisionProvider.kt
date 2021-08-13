package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionRevisionProvider(private val selectedConnectionProvider: SelectedConnectionProvider) : CheckScopedRevisions {
	override fun promiseRevision(): Promise<Int> =
		selectedConnectionProvider.promiseSessionConnection().eventually(RevisionStorage::promiseRevision)
}
