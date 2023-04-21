package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.RateLimitPromises
import com.namehillsoftware.handoff.promises.Promise

class RateControlledFilePropertiesProvider(private val inner: ProvideFreshLibraryFileProperties, private val rateLimiter: RateLimitPromises<Map<String, String>>): ProvideFreshLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		rateLimiter.limit { inner.promiseFileProperties(libraryId, serviceFile) }
}
