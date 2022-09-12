package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.RateLimitPromises
import com.namehillsoftware.handoff.promises.Promise

class RateControlledScopedFilePropertiesProvider(private val inner: ProvideScopedFileProperties, private val rateLimiter: RateLimitPromises<Map<String, String>>): ProvideScopedFileProperties {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		rateLimiter.limit { inner.promiseFileProperties(serviceFile) }
}
