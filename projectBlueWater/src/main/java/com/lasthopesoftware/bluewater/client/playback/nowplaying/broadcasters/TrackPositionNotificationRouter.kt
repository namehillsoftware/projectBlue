package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

class TrackPositionNotificationRouter(
	private val trackPositionUpdates: NotifyOfTrackPositionUpdates,
	private val registerApplicationMessages: RegisterForApplicationMessages,
) : (TrackPositionUpdate) -> Unit, AutoCloseable
{
	init {
		registerApplicationMessages.registerForClass(cls(), this)
	}

	override fun invoke(message: TrackPositionUpdate) {
		trackPositionUpdates.updateTrackPosition(message.filePosition.millis)
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}
}
