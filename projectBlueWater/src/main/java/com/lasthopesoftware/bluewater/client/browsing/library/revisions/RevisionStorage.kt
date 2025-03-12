package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.policies.caching.TimedExpirationPromiseCache
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.net.URL

internal object RevisionStorage : HasServerRevisionData {
	private val checkedExpirationTime = Duration.standardSeconds(30)
	private val revisionCache = TimedExpirationPromiseCache<String, Int>(checkedExpirationTime)

	override fun getOrSetRevisionData(url: URL, setter: (URL) -> Promise<Int>): Promise<Int> =
		revisionCache.getOrAdd(url.toString()) { setter(url) }
}
