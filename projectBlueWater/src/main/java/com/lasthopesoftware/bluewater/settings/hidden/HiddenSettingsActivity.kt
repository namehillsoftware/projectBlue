package com.lasthopesoftware.bluewater.settings.hidden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings

class HiddenSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

		setContent { HiddenSettings(HiddenSettingsViewModel(getApplicationSettingsRepository())) }
    }
}

//@Preview
@Composable
fun HiddenSettings(hiddenSettingsViewModel: HiddenSettingsViewModel) {
	Column {
		Row {
			Column {
				Checkbox(checked = hiddenSettingsViewModel.isUsingCustomCaching, onCheckedChange = { hiddenSettingsViewModel.saveSettings() })
			}

			Column {
				Text(text = "Use custom caching implementation")
			}
		}
	}
}

class HiddenSettingsViewModel(private val applicationSettingsRepository: HoldApplicationSettings) : ViewModel() {
	var isUsingCustomCaching by mutableStateOf(false)

	init {
	    applicationSettingsRepository
			.promiseApplicationSettings()
			.then { settings ->
				isUsingCustomCaching = settings.isUsingCustomCaching
			}
	}

	fun saveSettings() {
		applicationSettingsRepository
			.promiseApplicationSettings()
			.eventually { settings ->
				settings.isUsingCustomCaching = isUsingCustomCaching
				applicationSettingsRepository.promiseUpdatedSettings(settings)
			}
	}
}
