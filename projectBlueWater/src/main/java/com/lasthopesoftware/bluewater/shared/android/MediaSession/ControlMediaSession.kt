package com.lasthopesoftware.bluewater.shared.android.MediaSession

import android.app.PendingIntent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.namehillsoftware.handoff.promises.Promise

interface ControlMediaSession {
	fun activate()
	fun deactivate()
	fun setPlaybackState(playbackStateCompat: PlaybackStateCompat)
	fun setMetadata(metadata: MediaMetadataCompat)
	fun setSessionActivity(pendingIntent: PendingIntent)
	fun promiseInitialization(): Promise<Unit>
}
