package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages

class FileListItemNowPlayingRegistrar(private val messageRegistrar: RegisterForMessages) {

	companion object {
		private val intentFilter = IntentFilter(PlaylistEvents.onPlaylistTrackChange)
	}

	private val syncObj = Any()
	private val registeredHandlers = HashSet<FileListItemNowPlayingHandler>()

	fun registerNewHandler(receiver: ReceiveBroadcastEvents): AutoCloseable =
		FileListItemNowPlayingHandler(receiver).also {
			synchronized(syncObj) {
				messageRegistrar.registerReceiver(it, intentFilter)
				registeredHandlers.add(it)
			}
		}

	fun clear() {
		synchronized(syncObj) {
			registeredHandlers.forEach(messageRegistrar::unregisterReceiver)
			registeredHandlers.clear()
		}
	}

	private fun remove(handler: FileListItemNowPlayingHandler) {
		synchronized(syncObj) {
			messageRegistrar.unregisterReceiver(handler)
			registeredHandlers.remove(handler)
		}
	}

	private inner class FileListItemNowPlayingHandler(private val receiver: ReceiveBroadcastEvents) : BroadcastReceiver(), AutoCloseable {

		override fun onReceive(context: Context?, intent: Intent?) {
			if (context != null && intent != null) receiver.onReceive(context, intent)
		}

		override fun close() = remove(this)
	}
}
