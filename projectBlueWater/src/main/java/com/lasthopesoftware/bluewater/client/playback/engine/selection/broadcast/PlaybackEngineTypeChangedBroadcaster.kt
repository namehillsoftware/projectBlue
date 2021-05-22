package com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast

import android.content.Intent
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class PlaybackEngineTypeChangedBroadcaster(private val sendMessages: SendMessages) {
	fun broadcastPlaybackEngineTypeChanged(playbackEngineType: PlaybackEngineType) {
		val intent = Intent(playbackEngineTypeChanged)
		intent.putExtra(playbackEngineTypeKey, playbackEngineType.name)
		sendMessages.sendBroadcast(intent)
	}

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(
			PlaybackEngineTypeChangedBroadcaster::class.java
		)
		@kotlin.jvm.JvmField
		val playbackEngineTypeChanged = magicPropertyBuilder.buildProperty("playbackEngineTypeChanged")
		@kotlin.jvm.JvmField
		val playbackEngineTypeKey = magicPropertyBuilder.buildProperty("playbackEngineTypeKey")
	}
}
