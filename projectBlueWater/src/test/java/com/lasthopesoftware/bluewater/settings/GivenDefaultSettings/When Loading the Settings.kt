package com.lasthopesoftware.bluewater.settings.GivenDefaultSettings

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
				every { promiseApplicationSettings() } returns ApplicationSettings().toPromise()
			},
			mockk {
				every { promiseAllLibrarySettings() } returns Promise(emptyList())
			},
			mockk(),
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
		assertThat(mutt.isSyncOnPowerOnly.value).isFalse
	}

	@Test
	fun `then isSyncOnWifiOnly is correct`() {
		assertThat(mutt.isSyncOnWifiOnly.value).isFalse
	}

	@Test
	fun `then isVolumeLevelingEnabled is correct`() {
		assertThat(mutt.isVolumeLevelingEnabled.value).isFalse
	}

	@Test
	fun `then chosenLibraryId is correct`() {
		assertThat(mutt.chosenLibraryId.value).isNull()
	}

	@Test
	fun `then isPeakLevelNormalized is correct`() {
		assertThat(mutt.isPeakLevelNormalizeEnabled.value).isFalse
	}

	@Test
	fun `then the theme is correct`() {
		assertThat(mutt.theme.value).isEqualTo(ApplicationSettings.Theme.SYSTEM)
	}

	@Test
	fun `then the libraries are returned and sorted correctly`() {
		assertThat(mutt.libraries.value).isEmpty()
	}
}
