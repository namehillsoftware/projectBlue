package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.PendingIntent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.namehillsoftware.handoff.promises.Promise
import java.lang.AutoCloseable

class MediaSessionController(
	private val mediaSessionCompat: MediaSessionCompat,
	private val nowPlayingState: GetNowPlayingState,
) : ControlMediaSession, AutoCloseable {
	override fun promiseInitialization(): Promise<Unit> {
		return nowPlayingState
			.promiseActiveNowPlaying()
			.then { np ->
				np?.apply {
					if (playlistPosition > 0 && filePosition > 0) activate()
				}
			}
	}

	override fun activate() {
		if (!mediaSessionCompat.isActive)
			mediaSessionCompat.isActive = true
	}

	override fun deactivate() {
		if (mediaSessionCompat.isActive)
			mediaSessionCompat.isActive = false
	}

	override fun setPlaybackState(playbackStateCompat: PlaybackStateCompat) {
		mediaSessionCompat.setPlaybackState(playbackStateCompat)
	}

	override fun setMetadata(metadata: MediaMetadataCompat) {
		mediaSessionCompat.setMetadata(metadata)
	}

	override fun setSessionActivity(pendingIntent: PendingIntent) {
		mediaSessionCompat.setSessionActivity(pendingIntent)
	}

	override fun close() {
		deactivate()
		mediaSessionCompat.release()
	}
}
