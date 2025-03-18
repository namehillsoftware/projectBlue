package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.caching.TimedExpirationPromiseCache
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class CachedLibraryRevisionProvider(private val inner: CheckRevisions) : CheckRevisions {
	private val checkedExpirationTime = Duration.standardSeconds(30)
	private val revisionCache = TimedExpirationPromiseCache<LibraryId, Int>(checkedExpirationTime)

	override fun promiseRevision(libraryId: LibraryId): Promise<Int> =
		revisionCache.getOrAdd(libraryId, inner::promiseRevision)
}
