package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.*
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
		registerApplicationMessages.registerForClass(cls<PlaylistTrackChange>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackStart>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackPaused>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackInterrupted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackStopped>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is PlaylistTrackChange -> playbackNotificationBroadcaster.notifyPlayingFileChanged(message.positionedFile.serviceFile)
			is PlaybackStart -> playbackNotificationBroadcaster.notifyPlaying()
			is PlaybackPaused -> playbackNotificationBroadcaster.notifyPaused()
			is PlaybackInterrupted -> playbackNotificationBroadcaster.notifyInterrupted()
			is PlaybackStopped -> playbackNotificationBroadcaster.notifyStopped()
		}
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}
}
