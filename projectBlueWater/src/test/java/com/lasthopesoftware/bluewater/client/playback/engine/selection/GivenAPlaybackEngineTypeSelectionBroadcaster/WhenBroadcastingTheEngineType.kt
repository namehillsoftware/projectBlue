package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAPlaybackEngineTypeSelectionBroadcaster

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenBroadcastingTheEngineType {

	companion object {
		private val recordingApplicationMessageBus = RecordingApplicationMessageBus()

		@BeforeClass
		@JvmStatic
		fun before() {
			PlaybackEngineTypeChangedBroadcaster(recordingApplicationMessageBus)
				.broadcastPlaybackEngineTypeChanged(PlaybackEngineType.ExoPlayer)
		}
	}

	@Test
	fun thenTheExoPlayerSelectionIsBroadcast() {
		assertThat(
			recordingApplicationMessageBus.recordedMessages.filterIsInstance<PlaybackEngineTypeChangedBroadcaster.PlaybackEngineTypeChanged>()
				.firstOrNull()
				?.playbackEngineType
		).isEqualTo(PlaybackEngineType.ExoPlayer)
	}
}
