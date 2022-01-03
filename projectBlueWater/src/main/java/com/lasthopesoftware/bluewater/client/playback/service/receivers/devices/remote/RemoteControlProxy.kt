package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster

class RemoteControlProxy(private val remoteBroadcaster: IRemoteBroadcaster) : BroadcastReceiver() {
	private val mappedEvents by lazy {
		mapOf<String, (Intent) -> Unit>(
			Pair(PlaylistEvents.onPlaylistTrackChange, ::onPlaylistChange),
			Pair(PlaylistEvents.onPlaylistPause) { remoteBroadcaster.setPaused() },
			Pair(PlaylistEvents.onPlaylistStart) { remoteBroadcaster.setPlaying() },
			Pair(PlaylistEvents.onPlaylistStop) { remoteBroadcaster.setStopped() },
			Pair(TrackPositionBroadcaster.trackPositionUpdate, ::onTrackPositionUpdate),
		)
	}

	fun registerForIntents(): Set<String> = mappedEvents.keys

	override fun onReceive(context: Context, intent: Intent) {
		intent.action?.let(mappedEvents::get)?.invoke(intent)
	}

	private fun onPlaylistChange(intent: Intent) {
		val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
		if (fileKey > 0) remoteBroadcaster.updateNowPlaying(ServiceFile(fileKey))
	}

	private fun onTrackPositionUpdate(intent: Intent) {
		val trackPosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
		if (trackPosition >= 0) remoteBroadcaster.updateTrackPosition(trackPosition)
	}
}
