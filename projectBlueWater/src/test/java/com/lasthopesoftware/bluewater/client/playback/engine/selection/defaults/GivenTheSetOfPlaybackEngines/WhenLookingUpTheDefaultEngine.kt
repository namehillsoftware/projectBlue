package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.GivenTheSetOfPlaybackEngines

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenLookingUpTheDefaultEngine {
	private val playbackEngineType by lazy {
		DefaultPlaybackEngineLookup().promiseDefaultEngineType().toExpiringFuture().get()
	}

	@Test
	fun `then it is exo player`() {
		assertThat(playbackEngineType)
			.isEqualTo(PlaybackEngineType.ExoPlayer)
	}
}
