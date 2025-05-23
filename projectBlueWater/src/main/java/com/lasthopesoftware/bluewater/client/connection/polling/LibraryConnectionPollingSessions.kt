package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class LibraryConnectionPollingSessions(
	private val inner: PollForLibraryConnections,
	private val connectionPollerLookup: ConcurrentHashMap<LibraryId, Lazy<Promise<LiveServerConnection>>> = sharedConnectionPollerLookup
) : PollForLibraryConnections {

	companion object {
		private val sharedConnectionPollerLookup = ConcurrentHashMap<LibraryId, Lazy<Promise<LiveServerConnection>>>()
	}

	override fun pollConnection(libraryId: LibraryId): Promise<LiveServerConnection> =
		connectionPollerLookup
			.getOrPut(libraryId) {
				lazy {
					inner.pollConnection(libraryId)
				}
			}
			.let {
				it.value.apply { must { _ -> connectionPollerLookup.remove(libraryId, it) } }
			}

	fun cancelActiveConnections() {
		connectionPollerLookup.values.forEach { it.value.cancel() }
	}
}
