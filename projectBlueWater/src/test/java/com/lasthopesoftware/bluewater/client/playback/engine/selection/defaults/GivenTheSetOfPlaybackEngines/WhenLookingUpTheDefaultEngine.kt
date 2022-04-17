package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.GivenTheSetOfPlaybackEngines

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenLookingUpTheDefaultEngine {
	@Test
	fun thenItIsExoPlayer() {
		assertThat(playbackEngineType)
			.isEqualTo(PlaybackEngineType.ExoPlayer)
	}

	companion object {
		private var playbackEngineType: PlaybackEngineType? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			playbackEngineType =
				ExpiringFuturePromise(DefaultPlaybackEngineLookup().promiseDefaultEngineType()).get()
		}
	}
}
