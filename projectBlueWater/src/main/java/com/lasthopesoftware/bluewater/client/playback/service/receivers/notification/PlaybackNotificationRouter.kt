package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
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
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.TrackChanged>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackStarted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackPaused>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackStopped>(), this)
		registerApplicationMessages.registerForClass(cls<FilePropertiesUpdatedMessage>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is PlaybackMessage.TrackChanged -> playbackNotificationBroadcaster.notifyPlayingFileChanged(message.positionedFile.serviceFile)
			is PlaybackMessage.PlaybackStarted -> playbackNotificationBroadcaster.notifyPlaying()
			is PlaybackMessage.PlaybackPaused -> playbackNotificationBroadcaster.notifyPaused()
			is PlaybackMessage.PlaybackInterrupted -> playbackNotificationBroadcaster.notifyInterrupted()
			is PlaybackMessage.PlaybackStopped -> playbackNotificationBroadcaster.notifyStopped()
			is FilePropertiesUpdatedMessage -> playbackNotificationBroadcaster.notifyPropertiesUpdated(message.urlServiceKey)
		}
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}
}
