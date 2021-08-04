package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.*

class RevisionChecker(private val libraryConnections: ProvideLibraryConnections) : CheckRevisions {

	companion object {
		private const val badRevision = -1
		private val checkedExpirationTime = Duration.standardSeconds(30).millis
		private val cachedRevisions: MutableMap<String, Int> = HashMap()
		private val lastRevisions: MutableMap<String, Long> = HashMap()

		@JvmStatic
		fun promiseRevision(context: Context, connectionProvider: IConnectionProvider): Promise<Int> =
			RevisionChecker(ConnectionSessionManager.get(context)).promiseRevision(connectionProvider)

		private fun getCachedRevision(connectionProvider: IConnectionProvider): Int {
			val serverUrl = connectionProvider.urlProvider.baseUrl?.toString() ?: return badRevision
			if (!cachedRevisions.containsKey(serverUrl)) cachedRevisions[serverUrl] = badRevision
			return cachedRevisions[serverUrl] ?: badRevision
		}
	}

	override fun promiseRevision(libraryId: LibraryId): Promise<Int> =
		libraryConnections.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider?.let(::promiseRevision) ?: badRevision.toPromise()
			}

	private fun promiseRevision(connectionProvider: IConnectionProvider): Promise<Int> =
		connectionProvider.urlProvider.baseUrl?.toString()
			?.let { baseServerUrl ->
				lastRevisions[baseServerUrl]
					?.takeIf { System.currentTimeMillis() - checkedExpirationTime < it }
					?.let { getCachedRevision(connectionProvider) }
					?.takeIf { it != badRevision }
					?.let { Promise(it) }
					?: connectionProvider
						.promiseResponse("Library/GetRevision")
						.then({ response ->
							response.body
								?.use { body -> body.byteStream().use(StandardRequest::fromInputStream) }
								?.let { standardRequest -> standardRequest.items["Sync"] }
								?.takeIf { revisionValue -> revisionValue.isNotEmpty() }
								?.also { revisionValue ->
									cachedRevisions[baseServerUrl] = Integer.valueOf(revisionValue)
									lastRevisions[baseServerUrl] = System.currentTimeMillis()
								}

							getCachedRevision(connectionProvider)
						}, { getCachedRevision(connectionProvider) })
		} ?: badRevision.toPromise()
}
