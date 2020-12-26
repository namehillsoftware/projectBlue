package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents
import java.util.*

class PlaybackNotificationRouter(private val playbackNotificationBroadcaster: NotifyOfPlaybackEvents) : BroadcastReceiver() {
	private val mappedEvents: MutableMap<String, (Intent) -> Unit>
	fun registerForIntents(): Set<String> {
		return mappedEvents.keys
	}

	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action ?: return
		val eventHandler = mappedEvents[action]
		eventHandler?.invoke(intent)
	}

	private fun onPlaylistChange(intent: Intent) {
		val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
		if (fileKey >= 0) playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(fileKey))
	}

	init {
		mappedEvents = HashMap(4)
		mappedEvents[PlaylistEvents.onPlaylistTrackChange] = ::onPlaylistChange
		mappedEvents[PlaylistEvents.onPlaylistPause] = { playbackNotificationBroadcaster.notifyPaused() }
		mappedEvents[PlaylistEvents.onPlaylistStart] = { playbackNotificationBroadcaster.notifyPlaying() }
		mappedEvents[PlaylistEvents.onPlaylistStop] = { playbackNotificationBroadcaster.notifyStopped() }
	}
}
