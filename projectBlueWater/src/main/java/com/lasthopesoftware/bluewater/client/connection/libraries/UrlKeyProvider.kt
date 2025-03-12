package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class UrlKeyProvider(private val provideLibraryConnections: ProvideLibraryConnections) : ProvideUrlKey {
	override fun <Key> promiseUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>?> =
		provideLibraryConnections
			.promiseLibraryConnection(libraryId)
			.cancelBackThen { connection, _ ->
				connection?.urlProvider?.baseUrl?.let { UrlKeyHolder(it, key) }
			}

	override fun <Key> promiseGuaranteedUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>> =
		promiseUrlKey(libraryId, key).cancelBackThen { it, _ -> it ?: throw UrlKeyNotReturnedException(libraryId, key) }
}
