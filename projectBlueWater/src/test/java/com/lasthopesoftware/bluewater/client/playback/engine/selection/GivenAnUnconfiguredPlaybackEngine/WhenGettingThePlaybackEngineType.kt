package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAnUnconfiguredPlaybackEngine

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenGettingThePlaybackEngineType {

	companion object {
		private var playbackEngineType: PlaybackEngineType? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val applicationSettings = mockk<HoldApplicationSettings>()
			every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings())

			val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(
				applicationSettings
			) { Promise(PlaybackEngineType.ExoPlayer) }
			playbackEngineType =
				FuturePromise(selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType()).get()
		}
	}

	@Test
	fun thenThePlaybackEngineTypeIsExoPlayer() {
		Assertions.assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
	}
}
