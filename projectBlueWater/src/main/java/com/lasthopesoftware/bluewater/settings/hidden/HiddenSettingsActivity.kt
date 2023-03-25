package com.lasthopesoftware.bluewater.settings.hidden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class HiddenSettingsActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent { HiddenSettings(HiddenSettingsViewModel(getApplicationSettingsRepository())) }
	}
}

@Composable
fun HiddenSettings(hiddenSettingsViewModel: HiddenSettingsViewModel) {
	Column {
		Row {
		}
	}
}

class HiddenSettingsViewModel(private val applicationSettingsRepository: HoldApplicationSettings) : ViewModel() {

	init {}

	private fun saveSettings(): Promise<ApplicationSettings> = applicationSettingsRepository
		.promiseApplicationSettings()
		.eventually { settings ->
			applicationSettingsRepository.promiseUpdatedSettings(settings)
		}
}
