package com.lasthopesoftware.bluewater.application.GivenApplicationSettings.AndTheSettingsChange

import com.lasthopesoftware.bluewater.ApplicationViewModel
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsUpdated
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
		val messageBus = RecordingApplicationMessageBus()
		Pair(
			messageBus,
			ApplicationViewModel(
				mockk {
					every { promiseApplicationSettings() } returns ApplicationSettings(
						theme = ApplicationSettings.Theme.DARK
					).toPromise() andThen ApplicationSettings(
						theme = ApplicationSettings.Theme.SYSTEM
					).toPromise()
				},
				messageBus,
			),
		)
	}

	@BeforeAll
	fun act() {
		val (messageBus, vm) = mut
		vm.loadSettings().toExpiringFuture().get()
		messageBus.sendMessage(ApplicationSettingsUpdated)
	}

	@Test
	fun `then the theme is correct`() {
		assertThat(mut.second.theme.value).isEqualTo(ApplicationSettings.Theme.SYSTEM)
	}
}
