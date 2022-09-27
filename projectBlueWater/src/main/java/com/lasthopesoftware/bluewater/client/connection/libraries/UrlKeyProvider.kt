package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

class UrlKeyProvider(private val provideLibraryConnections: ProvideLibraryConnections) : ProvideUrlKey {
	override fun <Key> promiseUrlKey(libraryId: LibraryId, key: Key): Promise<UrlKeyHolder<Key>?> =
		provideLibraryConnections
			.promiseLibraryConnection(libraryId)
			.then { connection ->
				connection?.urlProvider?.baseUrl?.let { UrlKeyHolder(it, key) }
			}
}
