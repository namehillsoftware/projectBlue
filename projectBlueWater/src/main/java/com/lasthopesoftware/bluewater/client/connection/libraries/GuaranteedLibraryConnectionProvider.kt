package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.ProxyPromise

class GuaranteedLibraryConnectionProvider(
	private val libraryConnectionProvider: ProvideLibraryConnections
) : ProvideGuaranteedLibraryConnections {
	override fun promiseLibraryConnection(libraryId: LibraryId): Promise<IConnectionProvider> = ProxyPromise { cp ->
		libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.also(cp::doCancel)
			.then { it ->
				it ?: throw ConnectionUnavailableException(libraryId)
			}
	}
}
