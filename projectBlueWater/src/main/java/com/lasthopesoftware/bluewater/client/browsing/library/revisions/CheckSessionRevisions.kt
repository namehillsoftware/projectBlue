package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.namehillsoftware.handoff.promises.Promise

interface CheckSessionRevisions {
	fun promiseRevision(): Promise<Int>
}
