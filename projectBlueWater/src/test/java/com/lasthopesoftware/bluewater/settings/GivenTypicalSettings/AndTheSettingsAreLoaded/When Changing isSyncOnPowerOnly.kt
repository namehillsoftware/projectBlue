package com.lasthopesoftware.bluewater.settings.GivenTypicalSettings.AndTheSettingsAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
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

class `When Changing isSyncOnPowerOnly` {
	private var savedApplicationSettings: ApplicationSettings? = null
	private var isSyncScheduled = false

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
			mockk {
				every { scheduleSync() } answers {
					isSyncScheduled = true
					Promise.empty()
				}
			}
		)
	}

	@BeforeAll
	fun act() {
		mutt.apply {
			loadSettings().toExpiringFuture().get()

			promiseSyncOnPowerChange(!isSyncOnPowerOnly.value).toExpiringFuture().get()
		}
	}

	@Test
	fun `then a sync is scheduled`() {
		assertThat(isSyncScheduled).isTrue
	}

	@Test
	fun `then isSyncOnPowerOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnPowerOnly).isTrue
	}

	@Test
	fun `then isSyncOnWifiOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnWifiOnly).isTrue
	}

	@Test
	fun `then isVolumeLevelingEnabled is correct`() {
		assertThat(savedApplicationSettings?.isVolumeLevelingEnabled).isTrue
	}

	@Test
	fun `then chosenLibraryId is correct`() {
		assertThat(savedApplicationSettings?.chosenLibraryId).isEqualTo(95)
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
