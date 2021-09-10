package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.namehillsoftware.handoff.promises.Promise

class FakeScopedRevisionProvider(private val version: Int) : CheckScopedRevisions {
	override fun promiseRevision(): Promise<Int> = Promise(version)
}
