package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

interface ProvideGuaranteedLibraryConnections {
	fun promiseLibraryConnection(libraryId: LibraryId): Promise<ProvideConnections>
	fun promiseLibraryAccess(libraryId: LibraryId): Promise<RemoteLibraryAccess>
	fun <T> promiseKey(libraryId: LibraryId, key: T): Promise<UrlKeyHolder<T>>
}
