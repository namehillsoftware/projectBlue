package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAPlaybackEngineTypeSelectionBroadcaster

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenBroadcastingTheEngineType {

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()

	@BeforeAll
	fun act() {
		PlaybackEngineTypeChangedBroadcaster(recordingApplicationMessageBus)
			.broadcastPlaybackEngineTypeChanged(PlaybackEngineType.ExoPlayer)
	}

	@Test
	fun `then the exo player selection is broadcast`() {
		assertThat(
			recordingApplicationMessageBus.recordedMessages.filterIsInstance<PlaybackEngineTypeChangedBroadcaster.PlaybackEngineTypeChanged>()
				.firstOrNull()
				?.playbackEngineType
		).isEqualTo(PlaybackEngineType.ExoPlayer)
	}
}
