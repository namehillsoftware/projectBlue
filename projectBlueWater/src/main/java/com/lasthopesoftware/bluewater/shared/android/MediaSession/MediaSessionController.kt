package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MediaSessionController(private val mediaSessionCompat: MediaSessionCompat) : ControlMediaSession {
	override fun setPlaybackState(playbackStateCompat: PlaybackStateCompat) {
		mediaSessionCompat.setPlaybackState(playbackStateCompat)
	}

	override fun setMetadata(metadata: MediaMetadataCompat) {
		mediaSessionCompat.setMetadata(metadata)
	}
}
