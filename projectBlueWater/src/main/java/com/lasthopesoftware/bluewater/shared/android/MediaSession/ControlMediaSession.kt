package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat

interface ControlMediaSession {
	fun setPlaybackState(playbackStateCompat: PlaybackStateCompat)
	fun setMetadata(metadata: MediaMetadataCompat)
}
