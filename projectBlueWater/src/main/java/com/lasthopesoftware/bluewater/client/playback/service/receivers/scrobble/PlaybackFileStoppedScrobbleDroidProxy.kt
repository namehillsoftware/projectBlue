package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage

class PlaybackFileStoppedScrobbleDroidProxy(private val context: Context, private val scrobbleIntentProvider: ScrobbleIntentProvider) :
		(LibraryPlaybackMessage.TrackCompleted) -> Unit {

	override fun invoke(p1: LibraryPlaybackMessage.TrackCompleted) {
		context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false))
	}
}
