package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAnUnconfiguredPlaybackEngine

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingThePlaybackEngineType {

	private val playbackEngineType by lazy {
		val applicationSettings = mockk<HoldApplicationSettings>()
		every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings())
		every { applicationSettings.promiseUpdatedSettings(any()) } answers { Promise(firstArg<ApplicationSettings>()) }

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
