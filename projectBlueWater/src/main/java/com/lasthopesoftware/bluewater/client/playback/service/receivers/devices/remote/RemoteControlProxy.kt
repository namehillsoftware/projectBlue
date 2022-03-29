package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
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
	    registerForApplicationMessages.registerForClass(cls<PlaybackMessage.TrackChanged>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackStarted>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackPaused>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), this)
		registerForApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackStopped>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is TrackPositionUpdate -> onTrackPositionUpdate(message)
			is PlaybackMessage.TrackChanged -> onPlaylistChange(message)
			is PlaybackMessage.PlaybackStarted -> remoteBroadcaster.setPlaying()
			is PlaybackMessage.PlaybackPaused, PlaybackMessage.PlaybackInterrupted -> remoteBroadcaster.setPaused()
			is PlaybackMessage.PlaybackStopped -> remoteBroadcaster.setStopped()
		}
	}

	override fun close() {
		registerForApplicationMessages.unregisterReceiver(this)
	}

	private fun onPlaylistChange(playlistTrackChanged: PlaybackMessage.TrackChanged) {
		remoteBroadcaster.updateNowPlaying(playlistTrackChanged.positionedFile.serviceFile)
	}

	private fun onTrackPositionUpdate(trackPositionUpdate: TrackPositionUpdate) {
		remoteBroadcaster.updateTrackPosition(trackPositionUpdate.filePosition.millis)
	}
}
