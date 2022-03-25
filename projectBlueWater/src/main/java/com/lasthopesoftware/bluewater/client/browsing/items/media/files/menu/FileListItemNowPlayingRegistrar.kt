package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackChange
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver

class FileListItemNowPlayingRegistrar(private val messageRegistrar: RegisterForApplicationMessages) {

	private val syncObj = Any()
	private val registeredHandlers = HashSet<(PlaylistTrackChange) -> Unit>()

	fun registerNewHandler(receiver: (PlaylistTrackChange) -> Unit): AutoCloseable =
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

	private fun remove(receiver: (PlaylistTrackChange) -> Unit) {
		synchronized(syncObj) {
			messageRegistrar.unregisterReceiver(receiver)
			registeredHandlers.remove(receiver)
		}
	}

	private inner class FileListItemNowPlayingHandler(private val receiver: (PlaylistTrackChange) -> Unit) : AutoCloseable {
		override fun close() = remove(receiver)
	}
}
