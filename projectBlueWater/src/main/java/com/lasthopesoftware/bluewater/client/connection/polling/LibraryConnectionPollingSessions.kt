package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class LibraryConnectionPollingSessions(
	private val inner: PollForLibraryConnections,
	private val connectionPollerLookup: ConcurrentHashMap<LibraryId, Lazy<Promise<IConnectionProvider>>> = sharedConnectionPollerLookup
) : PollForLibraryConnections {

	companion object {
		private val sharedConnectionPollerLookup = ConcurrentHashMap<LibraryId, Lazy<Promise<IConnectionProvider>>>()
	}

	override fun pollConnection(libraryId: LibraryId): Promise<IConnectionProvider> =
		connectionPollerLookup
			.getOrPut(libraryId) {
				lazy {
					inner.pollConnection(libraryId)
				}
			}
			.let {
				it.value.apply { must { connectionPollerLookup.remove(libraryId, it) } }
			}

	fun cancelActiveConnections() {
		connectionPollerLookup.values.forEach { it.value.cancel() }
	}
}
