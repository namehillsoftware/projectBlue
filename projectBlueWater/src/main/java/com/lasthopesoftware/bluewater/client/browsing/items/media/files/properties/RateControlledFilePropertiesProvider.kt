package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.RateLimitPromises
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise

class RateControlledFilePropertiesProvider(private val inner: ProvideScopedFileProperties, private val rateLimiter: RateLimitPromises<Map<String, String>>): ProvideScopedFileProperties {
	companion object {
		private val emptyPropertiesPromise by lazy { Promise(emptyMap<String, String>()) }
	}

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		rateLimiter.limit {
			CancellableProxyPromise { cp ->
				if (cp.isCancelled) emptyPropertiesPromise
				else inner.promiseFileProperties(serviceFile)
			}
		}
}
