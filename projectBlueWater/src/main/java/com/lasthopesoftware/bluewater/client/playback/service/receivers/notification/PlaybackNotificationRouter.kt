package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistMessages
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

class PlaybackNotificationRouter(
	private val playbackNotificationBroadcaster: NotifyOfPlaybackEvents,
	private val registerApplicationMessages: RegisterForApplicationMessages
) :
	(ApplicationMessage) -> Unit,
	AutoCloseable
{
	init {
		registerApplicationMessages.registerForClass(cls<PlaylistMessages.TrackChanged>(), this)
		registerApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackStarted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackPaused>(), this)
		registerApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackInterrupted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaylistMessages.PlaybackStopped>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is PlaylistMessages.TrackChanged -> playbackNotificationBroadcaster.notifyPlayingFileChanged(message.positionedFile.serviceFile)
			is PlaylistMessages.PlaybackStarted -> playbackNotificationBroadcaster.notifyPlaying()
			is PlaylistMessages.PlaybackPaused -> playbackNotificationBroadcaster.notifyPaused()
			is PlaylistMessages.PlaybackInterrupted -> playbackNotificationBroadcaster.notifyInterrupted()
			is PlaylistMessages.PlaybackStopped -> playbackNotificationBroadcaster.notifyStopped()
		}
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}
}
