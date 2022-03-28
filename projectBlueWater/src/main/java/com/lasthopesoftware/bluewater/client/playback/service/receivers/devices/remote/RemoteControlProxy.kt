package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.*
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

class RemoteControlProxy(private val registerForApplicationMessages: RegisterForApplicationMessages, private val remoteBroadcaster: IRemoteBroadcaster) :
	(ApplicationMessage) -> Unit,
	AutoCloseable
{
	init {
	    registerForApplicationMessages.registerForClass(cls<TrackPositionUpdate>(), this)
	    registerForApplicationMessages.registerForClass(cls<PlaylistTrackChange>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackStart>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackPaused>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackInterrupted>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackStopped>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is TrackPositionUpdate -> onTrackPositionUpdate(message)
			is PlaylistTrackChange -> onPlaylistChange(message)
			is PlaybackStart -> remoteBroadcaster.setPlaying()
			is PlaybackPaused, PlaybackInterrupted -> remoteBroadcaster.setPaused()
			is PlaybackStopped -> remoteBroadcaster.setStopped()
		}
	}

	override fun close() {
		registerForApplicationMessages.unregisterReceiver(this)
	}

	private fun onPlaylistChange(playlistTrackChange: PlaylistTrackChange) {
		remoteBroadcaster.updateNowPlaying(playlistTrackChange.positionedFile.serviceFile)
	}

	private fun onTrackPositionUpdate(trackPositionUpdate: TrackPositionUpdate) {
		remoteBroadcaster.updateTrackPosition(trackPositionUpdate.filePosition.millis)
	}
}
