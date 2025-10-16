package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAnUnconfiguredPlaybackEngine

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingThePlaybackEngineType {

	private val playbackEngineType by lazy {
		val applicationSettings = mockk<HoldApplicationFeatureConfiguration> {
			every { promiseFeatureConfiguration() } returns Promise(ApplicationFeatureConfiguration())
			every { promiseUpdatedFeatureConfiguration(any()) } answers { Promise(firstArg<ApplicationFeatureConfiguration>()) }
		}
		val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(
			applicationSettings,
			mockk {
				every { promiseDefaultEngineType() } returns Promise(PlaybackEngineType.ExoPlayer)
			})

		selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType().toExpiringFuture().get()
	}

	@Test
	fun `then the playback engine type is exo player`() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
	}
}
