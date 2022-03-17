package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages

class FileListItemNowPlayingRegistrar(private val messageRegistrar: RegisterForMessages) {

	companion object {
		private val intentFilter = IntentFilter(PlaylistEvents.onPlaylistTrackChange)
	}

	private val syncObj = Any()
	private val registeredHandlers = HashSet<ReceiveBroadcastEvents>()

	fun registerNewHandler(receiver: ReceiveBroadcastEvents): AutoCloseable =
		FileListItemNowPlayingHandler(receiver).also {
			synchronized(syncObj) {
				messageRegistrar.registerReceiver(receiver, intentFilter)
				registeredHandlers.add(receiver)
			}
		}

	fun clear() {
		synchronized(syncObj) {
			registeredHandlers.forEach(messageRegistrar::unregisterReceiver)
			registeredHandlers.clear()
		}
	}

	private fun remove(receiver: ReceiveBroadcastEvents) {
		synchronized(syncObj) {
			messageRegistrar.unregisterReceiver(receiver)
			registeredHandlers.remove(receiver)
		}
	}

	private inner class FileListItemNowPlayingHandler(private val receiver: ReceiveBroadcastEvents) : AutoCloseable {
		override fun close() = remove(receiver)
	}
}
