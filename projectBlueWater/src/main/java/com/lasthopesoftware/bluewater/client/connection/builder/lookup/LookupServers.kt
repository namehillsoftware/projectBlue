package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface LookupServers {
	fun promiseServerInformation(libraryId: LibraryId): Promise<ServerInfo?>
}
