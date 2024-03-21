package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.namehillsoftware.handoff.promises.Promise

interface ProvideGuaranteedLibraryConnections {
	fun promiseLibraryConnection(libraryId: LibraryId): Promise<ProvideConnections>
}
