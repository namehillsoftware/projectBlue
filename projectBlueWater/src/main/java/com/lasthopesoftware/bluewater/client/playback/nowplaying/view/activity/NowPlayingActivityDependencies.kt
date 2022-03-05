package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity

import android.os.Handler
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageBus

class NowPlayingActivityDependencies(private val nowPlayingActivity: NowPlayingActivity) : AutoCloseable {
	private val lazyPlaylistMessages = lazy { TypedMessageBus<NowPlayingPlaylistMessage>(handler) }

	val handler by lazy { Handler(nowPlayingActivity.mainLooper) }
	val playlistMessages
		get() = lazyPlaylistMessages.value

	override fun close() {
		if (lazyPlaylistMessages.isInitialized()) lazyPlaylistMessages.value.close()
	}
}
