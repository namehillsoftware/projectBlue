package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.namehillsoftware.handoff.promises.Promise

class GuaranteedLibraryConnectionProvider(
	private val libraryConnectionProvider: ProvideLibraryConnections
) : ProvideGuaranteedLibraryConnections {
	override fun promiseLibraryConnection(libraryId: LibraryId): Promise<ProvideConnections> = Promise.Proxy { cp ->
		libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.also(cp::doCancel)
			.then { it ->
				it ?: throw ConnectionUnavailableException(libraryId)
			}
	}
}
