package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.namehillsoftware.handoff.promises.Promise

class LiveNowPlayingLookup(private val inner: GetNowPlayingState) : BroadcastReceiver(), GetNowPlayingState {
	private var trackedPosition: Long? = null

	override fun promiseNowPlaying(): Promise<NowPlaying?> =
		inner
			.promiseNowPlaying()
			.then { np -> np?.apply { filePosition = trackedPosition ?: filePosition } }

	override fun onReceive(context: Context?, intent: Intent?) {
		trackedPosition = when (intent?.action) {
			TrackPositionBroadcaster.trackPositionUpdate -> intent
				.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
				.takeIf { it > -1 }
			PlaylistEvents.onPlaylistTrackChange -> null
			else -> trackedPosition
		}
	}
}
