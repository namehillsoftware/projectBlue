package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.policies.caching.TimedExpirationPromiseCache
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

internal object RevisionStorage {
	private const val badRevision = -1
	private val badRevisionPromise = Promise(badRevision)
	private val checkedExpirationTime = Duration.standardSeconds(30)
	private val expiringRevisionCache = TimedExpirationPromiseCache<String, Int>(checkedExpirationTime)

	internal fun promiseRevision(connectionProvider: ProvideConnections?): Promise<Int> {
		connectionProvider ?: return badRevisionPromise

		val urlProvider = connectionProvider.urlProvider
		return expiringRevisionCache.getOrAdd(urlProvider.baseUrl.toString()) {
				connectionProvider
					.promiseResponse("Library/GetRevision")
					.then { response ->
						response.body
							.use { body -> body.byteStream().use(StandardResponse::fromInputStream) }
							?.let { standardRequest -> standardRequest.items["Sync"] }
							?.takeIf { revisionValue -> revisionValue.isNotEmpty() }!!
							.toInt()
					}
			}
			.then(
				{ i -> i },
				{ badRevision }
			)
	}
}
