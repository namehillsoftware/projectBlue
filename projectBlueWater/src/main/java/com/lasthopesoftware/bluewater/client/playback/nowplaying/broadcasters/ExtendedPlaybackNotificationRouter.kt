package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

class ExtendedPlaybackNotificationRouter(
	private val trackPositionUpdates: NotifyOfTrackPositionUpdates,
	private val innerRouter: (ApplicationMessage) -> Unit,
	private val registerApplicationMessages: RegisterForApplicationMessages,
) : (ApplicationMessage) -> Unit, AutoCloseable
{
	init {
		registerApplicationMessages.registerForClass(cls<TrackPositionUpdate>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		if (message is TrackPositionUpdate) trackPositionUpdates.updateTrackPosition(message.filePosition.millis)
		else innerRouter(message)
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}
}
