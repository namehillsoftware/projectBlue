package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import java.util.*

class RemoteControlProxy(private val remoteBroadcaster: IRemoteBroadcaster) : BroadcastReceiver() {
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
		if (fileKey > 0) remoteBroadcaster.updateNowPlaying(ServiceFile(fileKey))
	}

	private fun onTrackPositionUpdate(intent: Intent) {
		val trackPosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
		if (trackPosition >= 0) remoteBroadcaster.updateTrackPosition(trackPosition)
	}

	init {
		mappedEvents = HashMap(5)
		mappedEvents[PlaylistEvents.onPlaylistTrackChange] = { intent -> onPlaylistChange(intent) }
		mappedEvents[PlaylistEvents.onPlaylistPause] = { remoteBroadcaster.setPaused() }
		mappedEvents[PlaylistEvents.onPlaylistStart] = { remoteBroadcaster.setPlaying() }
		mappedEvents[PlaylistEvents.onPlaylistStop] = { remoteBroadcaster.setStopped() }
		mappedEvents[TrackPositionBroadcaster.trackPositionUpdate] = { intent -> onTrackPositionUpdate(intent) }
	}
}
