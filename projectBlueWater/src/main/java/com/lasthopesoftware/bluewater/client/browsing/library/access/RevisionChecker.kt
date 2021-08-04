package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.*

class RevisionChecker private constructor(private val connectionProvider: IConnectionProvider) {

	companion object {
		private const val badRevision = -1
		private val checkedExpirationTime = Duration.standardSeconds(30).millis
		private val cachedRevisions: MutableMap<String, Int> = HashMap()
		private val lastRevisions: MutableMap<String, Long> = HashMap()

		@JvmStatic
		fun promiseRevision(connectionProvider: IConnectionProvider): Promise<Int> =
			RevisionChecker(connectionProvider).revision

		private fun getCachedRevision(connectionProvider: IConnectionProvider): Int {
			val serverUrl = connectionProvider.urlProvider.baseUrl ?: return badRevision
			if (!cachedRevisions.containsKey(serverUrl)) cachedRevisions[serverUrl] = badRevision
			return cachedRevisions[serverUrl] ?: badRevision
		}
	}

	private val revision: Promise<Int>
		get() {
			val baseServerUrl = connectionProvider.urlProvider.baseUrl ?: return badRevision.toPromise()
			return lastRevisions[baseServerUrl]
				?.takeIf { System.currentTimeMillis() - checkedExpirationTime < it }
				?.let { getCachedRevision(connectionProvider) }
				?.takeIf { it != badRevision }
				?.let { Promise(it) }
				?: connectionProvider.promiseResponse("Library/GetRevision")
				.then({ response ->
					response.body?.use {
						it.byteStream().use { stream ->
							StandardRequest.fromInputStream(stream)
								?.let { standardRequest -> standardRequest.items["Sync"] }
								?.takeIf { revisionValue -> revisionValue.isNotEmpty() }
								?.let { revisionValue ->
									cachedRevisions[baseServerUrl] = Integer.valueOf(revisionValue)
									lastRevisions[baseServerUrl] = System.currentTimeMillis()
								}
						}
					}

					getCachedRevision(connectionProvider)
				}, { getCachedRevision(connectionProvider) })
		}
}
