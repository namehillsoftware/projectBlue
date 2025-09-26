package com.lasthopesoftware.bluewater.settings.GivenTypicalSettings

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

class `When Loading the Settings` {
	private val mutt by lazy {
		ApplicationSettingsViewModel(
			mockk {
				every { promiseApplicationSettings() } returns Promise(
					ApplicationSettings(
						isSyncOnPowerOnly = true,
						isSyncOnWifiOnly = true,
						isVolumeLevelingEnabled = true,
						isPeakLevelNormalizeEnabled = true,
						chosenLibraryId = 304,
						playbackEngineTypeName = PlaybackEngineType.ExoPlayer.name,
					)
				)
			},
			mockk {
				every { promiseSelectedPlaybackEngineType() } returns PlaybackEngineType.ExoPlayer.toPromise()
			},
			mockk {
				every { promiseAllLibrarySettings() } returns Promise(
					listOf(
						LibrarySettings(libraryId = LibraryId(504)),
						LibrarySettings(libraryId = LibraryId(395)),
						LibrarySettings(libraryId = LibraryId(304)),
						LibrarySettings(libraryId = LibraryId(661)),
					)
				)
			},
			mockk {
				every { promiseLibraryName(LibraryId(504)) } returns "RJvwnHp8".toPromise()
				every { promiseLibraryName(LibraryId(395)) } returns "sJF83GATo".toPromise()
				every { promiseLibraryName(LibraryId(304)) } returns "dNiotMiP".toPromise()
				every { promiseLibraryName(LibraryId(661)) } returns "Adutlms5vK7".toPromise()
			},
			RecordingApplicationMessageBus(),
			mockk(),
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
	fun `then isPeakLevelNormalized is correct`() {
		assertThat(mutt.isPeakLevelNormalizeEnabled.value).isTrue
	}

	@Test
	fun `then the libraries are returned and sorted correctly`() {
		assertThat(mutt.libraries.value).isEqualTo(
			listOf(
				Pair(LibraryId(304), "dNiotMiP"),
				Pair(LibraryId(395), "sJF83GATo"),
				Pair(LibraryId(504), "RJvwnHp8"),
				Pair(LibraryId(661), "Adutlms5vK7"),
			)
		)
	}
}
