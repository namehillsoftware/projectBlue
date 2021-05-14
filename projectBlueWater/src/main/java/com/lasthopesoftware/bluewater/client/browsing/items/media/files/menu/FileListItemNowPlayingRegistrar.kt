package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class FileListItemNowPlayingRegistrar(private val localBroadcastManager: LocalBroadcastManager) {
	fun registerNewHandler(fileListItem: FileListItemContainer, receiver: ReceiveBroadcastEvents): AutoCloseable {
		val fileListItemNowPlayingHandler = FileListItemNowPlayingHandler(fileListItem, receiver)
		localBroadcastManager.registerReceiver(fileListItemNowPlayingHandler, IntentFilter(PlaylistEvents.onPlaylistTrackChange))
		fileListItem.textViewContainer.addOnAttachStateChangeListener(fileListItemNowPlayingHandler)
		return fileListItemNowPlayingHandler
	}

	private class FileListItemNowPlayingHandler(fileListItem: FileListItemContainer, private val receiver: ReceiveBroadcastEvents) : BroadcastReceiver(), View.OnAttachStateChangeListener, AutoCloseable {
		private val fileTextViewContainer = fileListItem.textViewContainer
		private val localBroadcastManager = LocalBroadcastManager.getInstance(fileTextViewContainer.context)

		override fun onReceive(context: Context?, intent: Intent?) {
			if (context != null && intent != null) receiver.onReceive(context, intent)
		}

		override fun close() {
			localBroadcastManager.unregisterReceiver(this)
			fileTextViewContainer.removeOnAttachStateChangeListener(this)
		}

		override fun onViewDetachedFromWindow(v: View) {
			localBroadcastManager.unregisterReceiver(this)
		}

		override fun onViewAttachedToWindow(v: View) {}
	}
}
