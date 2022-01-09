package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents

class PlaybackNotificationRouter(private val playbackNotificationBroadcaster: NotifyOfPlaybackEvents) : BroadcastReceiver() {
	private val mappedEvents by lazy {
		mapOf(
			Pair(PlaylistEvents.onPlaylistTrackChange, ::onPlaylistChange),
			Pair(PlaylistEvents.onPlaylistPause, { playbackNotificationBroadcaster.notifyPaused() }),
			Pair(PlaylistEvents.onPlaylistInterrupted, { playbackNotificationBroadcaster.notifyInterrupted() }),
			Pair(PlaylistEvents.onPlaylistStart, { playbackNotificationBroadcaster.notifyPlaying() }),
			Pair(PlaylistEvents.onPlaylistStop, { playbackNotificationBroadcaster.notifyStopped() }),
		)
	}

	fun registerForIntents(): Set<String> = mappedEvents.keys

	override fun onReceive(context: Context, intent: Intent) {
		intent.action?.let(mappedEvents::get)?.invoke(intent)
	}

	private fun onPlaylistChange(intent: Intent) {
		val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
		if (fileKey >= 0) playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(fileKey))
	}
}
