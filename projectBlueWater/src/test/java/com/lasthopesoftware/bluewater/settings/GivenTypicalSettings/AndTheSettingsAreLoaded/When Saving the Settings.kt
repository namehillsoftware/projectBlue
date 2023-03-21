package com.lasthopesoftware.bluewater.settings.GivenTypicalSettings.AndTheSettingsAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Saving the Settings` {
	private var savedApplicationSettings: ApplicationSettings? = null

	private val mutt by lazy {
		ApplicationSettingsViewModel(
			mockk {
				every { promiseApplicationSettings() } returns Promise(
					ApplicationSettings(
						isSyncOnPowerOnly = false,
						isSyncOnWifiOnly = true,
						isVolumeLevelingEnabled = true,
						chosenLibraryId = 95,
						playbackEngineTypeName = PlaybackEngineType.ExoPlayer.name,
					)
				)

				every { promiseUpdatedSettings(any()) } answers {
					val settings = firstArg<ApplicationSettings>()
					savedApplicationSettings = settings
					Promise(settings)
				}
			},
			mockk {
				every { promiseSelectedPlaybackEngineType() } returns PlaybackEngineType.ExoPlayer.toPromise()
			},
			mockk {
				every { allLibraries } returns Promise(
					listOf(
						Library(_id = 585),
						Library(_id = 893),
						Library(_id = 72),
					)
				)
			},
			RecordingApplicationMessageBus(),
		)
	}

	@BeforeAll
	fun act() {
		mutt.apply {
			loadSettings().toExpiringFuture().get()

			isSyncOnWifiOnly.value = !isSyncOnWifiOnly.value
			isSyncOnPowerOnly.value = !isSyncOnPowerOnly.value
			isVolumeLevelingEnabled.value = !isVolumeLevelingEnabled.value
			chosenLibraryId.value = LibraryId(463)

			saveSettings().toExpiringFuture().get()
		}
	}

	@Test
	fun `then isSyncOnPowerOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnPowerOnly).isTrue
	}

	@Test
	fun `then isSyncOnWifiOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnWifiOnly).isFalse
	}

	@Test
	fun `then isVolumeLevelingEnabled is correct`() {
		assertThat(savedApplicationSettings?.isVolumeLevelingEnabled).isFalse
	}

	@Test
	fun `then chosenLibraryId is correct`() {
		assertThat(savedApplicationSettings?.chosenLibraryId).isEqualTo(463)
	}

	@Test
	fun `then the playbackEngineType is correct`() {
		assertThat(savedApplicationSettings?.playbackEngineTypeName).isEqualTo(PlaybackEngineType.ExoPlayer.name)
	}

	@Test
	fun `then the libraries are correct`() {
		assertThat(mutt.libraries.value).isEqualTo(
			listOf(
				Library(_id = 585),
				Library(_id = 893),
				Library(_id = 72),
			)
		)
	}
}
