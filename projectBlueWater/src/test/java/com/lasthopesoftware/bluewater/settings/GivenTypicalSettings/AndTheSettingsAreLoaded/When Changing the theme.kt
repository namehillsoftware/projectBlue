package com.lasthopesoftware.bluewater.settings.GivenTypicalSettings.AndTheSettingsAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Changing the theme` {
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
						theme = ApplicationSettings.Theme.LIGHT,
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
				every { promiseAllLibrarySettings() } returns Promise(
					listOf(
						LibrarySettings(libraryId = LibraryId(585)),
						LibrarySettings(libraryId = LibraryId(686)),
						LibrarySettings(libraryId = LibraryId(797)),
					)
				)
			},
			mockk {
				every { promiseLibraryName(LibraryId(585)) } returns "R9yt8fKe".toPromise()
				every { promiseLibraryName(LibraryId(686)) } returns "7aqNFmn".toPromise()
				every { promiseLibraryName(LibraryId(797)) } returns "MHcdrd3nR".toPromise()
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

			promiseThemeChange(ApplicationSettings.Theme.DARK).toExpiringFuture().get()
		}
	}

	@Test
	fun `then a sync is not scheduled`() {
		assertThat(isSyncScheduled).isFalse
	}

	@Test
	fun `then isSyncOnPowerOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnPowerOnly).isFalse
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
	fun `then isPeakLevelNormalized is correct`() {
		assertThat(savedApplicationSettings?.isPeakLevelNormalizeEnabled).isFalse
	}

	@Test
	fun `then the theme is correct`() {
		assertThat(savedApplicationSettings?.theme).isEqualTo(ApplicationSettings.Theme.DARK)
	}

	@Test
	fun `then the libraries are correct`() {
		assertThat(mutt.libraries.value).isEqualTo(
			listOf(
				Pair(LibraryId(585), "R9yt8fKe"),
				Pair(LibraryId(686), "7aqNFmn"),
				Pair(LibraryId(797), "MHcdrd3nR"),
			)
		)
	}
}
