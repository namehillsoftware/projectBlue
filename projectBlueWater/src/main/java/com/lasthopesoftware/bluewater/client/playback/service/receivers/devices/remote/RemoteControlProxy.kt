package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistMessages
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

class RemoteControlProxy(private val registerForApplicationMessages: RegisterForApplicationMessages, private val remoteBroadcaster: IRemoteBroadcaster) :
	(ApplicationMessage) -> Unit,
	AutoCloseable
{
	init {
	    registerForApplicationMessages.registerForClass(cls<TrackPositionUpdate>(), this)
	    registerForApplicationMessages.registerForClass(cls<PlaylistMessages.TrackChanged>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackStarted>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackPaused>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackInterrupted>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackStopped>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is TrackPositionUpdate -> onTrackPositionUpdate(message)
			is PlaylistMessages.TrackChanged -> onPlaylistChange(message)
			is PlaylistMessages.PlaybackStarted -> remoteBroadcaster.setPlaying()
			is PlaylistMessages.PlaybackPaused, PlaylistMessages.PlaybackInterrupted -> remoteBroadcaster.setPaused()
			is PlaylistMessages.PlaybackStopped -> remoteBroadcaster.setStopped()
		}
	}

	override fun close() {
		registerForApplicationMessages.unregisterReceiver(this)
	}

	private fun onPlaylistChange(playlistTrackChanged: PlaylistMessages.TrackChanged) {
		remoteBroadcaster.updateNowPlaying(playlistTrackChanged.positionedFile.serviceFile)
	}

	private fun onTrackPositionUpdate(trackPositionUpdate: TrackPositionUpdate) {
		remoteBroadcaster.updateTrackPosition(trackPositionUpdate.filePosition.millis)
	}
}
