package com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class PlaybackEngineTypeChangedBroadcaster(private val applicationMessages: SendApplicationMessages) {
	fun broadcastPlaybackEngineTypeChanged(playbackEngineType: PlaybackEngineType) {
		applicationMessages.sendMessage(PlaybackEngineTypeChanged(playbackEngineType))
	}

	class PlaybackEngineTypeChanged(val playbackEngineType: PlaybackEngineType) : ApplicationMessage
}
