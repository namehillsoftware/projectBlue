package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.PendingIntent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import java.lang.AutoCloseable

class MediaSessionController(private val mediaSessionCompat: MediaSessionCompat) : ControlMediaSession, AutoCloseable {
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
