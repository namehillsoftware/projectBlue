package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ScopedRevisionProvider(private val scopedConnection: IConnectionProvider) : CheckScopedRevisions {
	override fun promiseRevision(): Promise<Int> = RevisionStorage.promiseRevision(scopedConnection)
}
