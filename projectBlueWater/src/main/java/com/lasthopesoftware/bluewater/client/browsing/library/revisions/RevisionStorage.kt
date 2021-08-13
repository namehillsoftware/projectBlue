package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.*

internal object RevisionStorage {
	private const val badRevision = -1
	private val checkedExpirationTime = Duration.standardSeconds(30).millis
	private val cachedRevisions: MutableMap<String, Int> = HashMap()
	private val lastRevisions: MutableMap<String, Long> = HashMap()

	private fun getCachedRevision(urlProvider: IUrlProvider): Int {
		val serverUrl = urlProvider.baseUrl?.toString() ?: return badRevision
		if (!cachedRevisions.containsKey(serverUrl)) cachedRevisions[serverUrl] =
			badRevision
		return cachedRevisions[serverUrl] ?: badRevision
	}

	internal fun promiseRevision(connectionProvider: IConnectionProvider?): Promise<Int> {
		connectionProvider ?: return badRevision.toPromise()

		val urlProvider = connectionProvider.urlProvider
		return urlProvider.baseUrl?.toString()
			?.let { baseServerUrl ->
				lastRevisions[baseServerUrl]
					?.takeIf { System.currentTimeMillis() - checkedExpirationTime < it }
					?.let { getCachedRevision(urlProvider) }
					?.takeIf { it != badRevision }
					?.let { Promise(it) }
					?: connectionProvider
						.promiseResponse("Library/GetRevision")
						.then({ response ->
							response.body
								?.use { body -> body.byteStream().use(StandardRequest::fromInputStream) }
								?.let { standardRequest -> standardRequest.items["Sync"] }
								?.takeIf { revisionValue -> revisionValue.isNotEmpty() }
								?.let(Integer::valueOf)
								?.also { revisionValue ->
									cachedRevisions[baseServerUrl] = revisionValue
									lastRevisions[baseServerUrl] = System.currentTimeMillis()
								}
								?: getCachedRevision(urlProvider)
						}, { getCachedRevision(urlProvider) })
			} ?: badRevision.toPromise()
	}
}
