package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackChanged
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver

class FileListItemNowPlayingRegistrar(private val messageRegistrar: RegisterForApplicationMessages) {

	private val syncObj = Any()
	private val registeredHandlers = HashSet<(PlaylistTrackChanged) -> Unit>()

	fun registerNewHandler(receiver: (PlaylistTrackChanged) -> Unit): AutoCloseable =
		FileListItemNowPlayingHandler(receiver).also {
			synchronized(syncObj) {
				messageRegistrar.registerReceiver(receiver)
				registeredHandlers.add(receiver)
			}
		}

	fun clear() {
		synchronized(syncObj) {
			registeredHandlers.forEach(messageRegistrar::unregisterReceiver)
			registeredHandlers.clear()
		}
	}

	private fun remove(receiver: (PlaylistTrackChanged) -> Unit) {
		synchronized(syncObj) {
			messageRegistrar.unregisterReceiver(receiver)
			registeredHandlers.remove(receiver)
		}
	}

	private inner class FileListItemNowPlayingHandler(private val receiver: (PlaylistTrackChanged) -> Unit) : AutoCloseable {
		override fun close() = remove(receiver)
	}
}
