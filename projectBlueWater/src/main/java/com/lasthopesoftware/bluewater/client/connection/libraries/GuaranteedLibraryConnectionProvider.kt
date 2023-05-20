package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise

class GuaranteedLibraryConnectionProvider(
	private val libraryConnectionProvider: ProvideLibraryConnections
) : ProvideGuaranteedLibraryConnections {
	override fun promiseLibraryConnection(libraryId: LibraryId): Promise<IConnectionProvider> = CancellableProxyPromise { cp ->
		libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.also(cp::doCancel)
			.then {
				it ?: throw ConnectionUnavailableException(libraryId)
			}
	}
}
