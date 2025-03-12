package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class GuaranteedLibraryConnectionProvider(
	private val libraryConnectionProvider: ProvideLibraryConnections
) : ProvideGuaranteedLibraryConnections {
	override fun promiseLibraryConnection(libraryId: LibraryId): Promise<ProvideConnections> =
		libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.cancelBackThen { c, _ ->
				c ?: throw ConnectionUnavailableException(libraryId)
			}

	override fun promiseLibraryAccess(libraryId: LibraryId): Promise<RemoteLibraryAccess> =
		promiseLibraryConnection(libraryId).cancelBackThen { c, _ -> c.getDataAccess() }

	override fun <T> promiseKey(libraryId: LibraryId, key: T): Promise<UrlKeyHolder<T>> =
		promiseLibraryConnection(libraryId).cancelBackThen { c, _ -> c.getConnectionKey(key) }
}
