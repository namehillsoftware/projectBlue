package com.lasthopesoftware.bluewater.application.GivenApplicationSettings.AndNoThemeSet

import com.lasthopesoftware.bluewater.ApplicationViewModel
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading settings` {
	private val mut by lazy {
		ApplicationViewModel(
			mockk {
				every { promiseApplicationSettings() } returns ApplicationSettings().toPromise()
			},
			RecordingApplicationMessageBus(),
		)
	}

	@BeforeAll
	fun act() {
		mut.loadSettings().toExpiringFuture().get()
	}

	@Test
	fun `then the theme is correct`() {
		assertThat(mut.theme.value).isEqualTo(ApplicationSettings.Theme.SYSTEM)
	}
}
