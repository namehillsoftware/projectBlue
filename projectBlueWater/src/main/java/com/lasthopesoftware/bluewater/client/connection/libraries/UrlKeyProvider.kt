package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

class UrlKeyProvider(private val provideLibraryConnections: ProvideLibraryConnections) : ProvideUrlKey {
	override fun <Key> promiseUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>?> = Promise.Proxy { cp ->
		provideLibraryConnections
			.promiseLibraryConnection(libraryId)
			.also(cp::doCancel)
			.then { connection ->
				connection?.urlProvider?.baseUrl?.let { UrlKeyHolder(it, key) }
			}
	}

	override fun <Key> promiseGuaranteedUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>> = Promise.Proxy { cp ->
		promiseUrlKey(libraryId, key)
			.also(cp::doCancel)
			.then { it -> it ?: throw UrlKeyNotReturnedException(libraryId, key) }
	}
}
