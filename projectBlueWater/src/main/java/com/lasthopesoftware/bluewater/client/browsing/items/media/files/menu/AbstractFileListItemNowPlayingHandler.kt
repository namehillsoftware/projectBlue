package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents

/**
 * Created by david on 4/14/15.
 */
abstract class AbstractFileListItemNowPlayingHandler(fileListItem: FileListItemContainer) : BroadcastReceiver(), View.OnAttachStateChangeListener {
	private val fileTextViewContainer = fileListItem.textViewContainer
	private val localBroadcastManager = LocalBroadcastManager.getInstance(fileTextViewContainer.context)

	init {
		localBroadcastManager.registerReceiver(this, IntentFilter(PlaylistEvents.onPlaylistTrackChange))
		fileTextViewContainer.addOnAttachStateChangeListener(this)
	}

	fun release() {
		localBroadcastManager.unregisterReceiver(this)
		fileTextViewContainer.removeOnAttachStateChangeListener(this)
	}

	override fun onViewDetachedFromWindow(v: View) {
		localBroadcastManager.unregisterReceiver(this)
	}

	override fun onViewAttachedToWindow(v: View) {}
}
