package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class RemoteControlProxy(private val remoteBroadcaster: IRemoteBroadcaster) : ReceiveBroadcastEvents, (ApplicationMessage) -> Unit {
	private val mappedEvents by lazy {
		mapOf(
			Pair(PlaylistEvents.onPlaylistTrackChange, ::onPlaylistChange),
			Pair(PlaylistEvents.onPlaylistPause) { remoteBroadcaster.setPaused() },
			Pair(PlaylistEvents.onPlaylistInterrupted) { remoteBroadcaster.setPaused() },
			Pair(PlaylistEvents.onPlaylistStart) { remoteBroadcaster.setPlaying() },
			Pair(PlaylistEvents.onPlaylistStop) { remoteBroadcaster.setStopped() },
		)
	}

	private val typedEvents by lazy {
		mapOf<Class<*>, (ApplicationMessage) -> Unit>(
			Pair(cls<TrackPositionBroadcaster.TrackPositionUpdate>(), ::onTrackPositionUpdate)
		)
	}

	fun registerForIntents(): Set<String> = mappedEvents.keys

	@Suppress("UNCHECKED_CAST")
	fun typeRegistrations(): Set<Class<ApplicationMessage>> = typedEvents.keys as Set<Class<ApplicationMessage>>

	override fun onReceive(intent: Intent) {
		intent.action?.let(mappedEvents::get)?.invoke(intent)
	}

	override fun invoke(p1: ApplicationMessage) {
		typedEvents[p1.javaClass]?.invoke(p1)
	}

	private fun onPlaylistChange(intent: Intent) {
		val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
		if (fileKey > 0) remoteBroadcaster.updateNowPlaying(ServiceFile(fileKey))
	}

	private fun onTrackPositionUpdate(applicationMessage: ApplicationMessage) {
		val trackPositionUpdate = applicationMessage as? TrackPositionBroadcaster.TrackPositionUpdate ?: return
		remoteBroadcaster.updateTrackPosition(trackPositionUpdate.filePosition.millis)
	}
}
