package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class SessionRevisionProvider(private val selectedConnectionProvider: ProvideSelectedConnection) : CheckSessionRevisions {
	override fun promiseRevision(): Promise<Int> =
		selectedConnectionProvider
			.promiseSessionConnection()
			.eventually(RevisionStorage::promiseRevision)
}
