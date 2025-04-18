package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.policies.ExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingLibraryConnectionProvider(
	private val inner: ProvideLibraryConnections,
	private val policies: ExecutionPolicies
) : ProvideLibraryConnections {
	private val promisedLibraryConnection by lazy { policies.applyPolicy(inner::promiseLibraryConnection) }

	override fun promiseLibraryConnection(libraryId: LibraryId): Promise<LiveServerConnection?> =
		promisedLibraryConnection(libraryId)
}
