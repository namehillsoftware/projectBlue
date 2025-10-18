package com.lasthopesoftware.bluewater.settings.GivenTypicalSettings.AndTheSettingsAreLoaded.AndVolumeLevelingIsEnabled

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
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

class `When Changing isPeakLevelNormalizeEnabled` {
	private var savedApplicationSettings: ApplicationSettings? = null

	private val mutt by lazy {
		ApplicationSettingsViewModel(
			mockk {
				every { promiseApplicationSettings() } returns Promise(
					ApplicationSettings(
						isSyncOnPowerOnly = false,
						isSyncOnWifiOnly = false,
						isVolumeLevelingEnabled = false,
						isPeakLevelNormalizeEnabled = false,
						chosenLibraryId = 95,
					)
				)

				every { promiseUpdatedSettings(any()) } answers {
					val settings = firstArg<ApplicationSettings>()
					savedApplicationSettings = settings
					Promise(settings)
				}
			},
            mockk {
				every { promiseAllLibrarySettings() } returns Promise(
					listOf(
						LibrarySettings(libraryId = LibraryId(585)),
						LibrarySettings(libraryId = LibraryId(893)),
						LibrarySettings(libraryId = LibraryId(72)),
					)
				)
			},
			mockk {
				every { promiseLibraryName(LibraryId(585)) } returns "R9yt8fKe".toPromise()
				every { promiseLibraryName(LibraryId(893)) } returns "7aqNFmn".toPromise()
				every { promiseLibraryName(LibraryId(72)) } returns "MHcdrd3nR".toPromise()
			},
			RecordingApplicationMessageBus(),
			mockk(),
		)
	}

	private var isPeakLevelEditableBeforeEnablingVolumeLeveling = false

	@BeforeAll
	fun act() {
		mutt.apply {
			loadSettings().toExpiringFuture().get()

			isPeakLevelEditableBeforeEnablingVolumeLeveling = isPeakLevelNormalizeEditable.value
			promiseVolumeLevelingEnabledChange(true).toExpiringFuture().get()
			promisePeakLevelNormalizeEnabledChange(true).toExpiringFuture().get()
		}
	}

	@Test
	fun `then isSyncOnPowerOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnPowerOnly).isFalse
	}

	@Test
	fun `then isSyncOnWifiOnly is correct`() {
		assertThat(savedApplicationSettings?.isSyncOnWifiOnly).isFalse
	}

	@Test
	fun `then isVolumeLevelingEnabled is correct`() {
		assertThat(savedApplicationSettings?.isVolumeLevelingEnabled).isTrue
	}

	@Test
	fun `then isPeakLevelEditableBeforeEnablingVolumeLeveling is correct`() {
		assertThat(isPeakLevelEditableBeforeEnablingVolumeLeveling).isFalse
	}

	@Test
	fun `then isPeakLevelNormalizeEditable is correct`() {
		assertThat(mutt.isPeakLevelNormalizeEditable.value).isTrue
	}

	@Test
	fun `then isPeakLevelNormalized is correct`() {
		assertThat(savedApplicationSettings?.isPeakLevelNormalizeEnabled).isTrue
	}

	@Test
	fun `then chosenLibraryId is correct`() {
		assertThat(savedApplicationSettings?.chosenLibraryId).isEqualTo(95)
	}

	@Test
	fun `then the libraries are correct`() {
		assertThat(mutt.libraries.value).isEqualTo(
			listOf(
				Pair(LibraryId(72), "MHcdrd3nR"),
				Pair(LibraryId(585), "R9yt8fKe"),
				Pair(LibraryId(893), "7aqNFmn"),
			)
		)
	}
}
