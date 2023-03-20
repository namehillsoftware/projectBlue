package com.lasthopesoftware.bluewater.settings.GivenTypicalSettings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Loading the Settings` {
	private val mutt by lazy {
		ApplicationSettingsViewModel(
			mockk {
				every { promiseApplicationSettings() } returns Promise(
					ApplicationSettings(
						isSyncOnPowerOnly = true,
						isSyncOnWifiOnly = true,
						isVolumeLevelingEnabled = true,
						chosenLibraryId = 304,
						playbackEngineTypeName = PlaybackEngineType.ExoPlayer.name,
					)
				)
			},
			mockk {
				every { promiseSelectedPlaybackEngineType() } returns PlaybackEngineType.ExoPlayer.toPromise()
			},
			mockk {
				every { allLibraries } returns Promise(
					listOf(
						Library(_id = 504),
						Library(_id = 395),
						Library(_id = 304),
					)
				)
			},
		)
	}

	@BeforeAll
	fun act() {
		mutt.loadSettings().toExpiringFuture().get()
	}

	@Test
	fun `then isSyncOnPowerOnly is correct`() {
		assertThat(mutt.isSyncOnPowerOnly.value).isTrue
	}

	@Test
	fun `then isSyncOnWifiOnly is correct`() {
		assertThat(mutt.isSyncOnWifiOnly.value).isTrue
	}

	@Test
	fun `then isVolumeLevelingEnabled is correct`() {
		assertThat(mutt.isVolumeLevelingEnabled.value).isTrue
	}

	@Test
	fun `then chosenLibraryId is correct`() {
		assertThat(mutt.chosenLibraryId.value).isEqualTo(LibraryId(304))
	}

	@Test
	fun `then the playbackEngineType is correct`() {
		assertThat(mutt.playbackEngineType.value).isEqualTo(PlaybackEngineType.ExoPlayer)
	}

	@Test
	fun `then the libraries are correct`() {
		assertThat(mutt.libraries.value).isEqualTo(
			listOf(
				Library(_id = 504),
				Library(_id = 395),
				Library(_id = 304),
			)
		)
	}
}
