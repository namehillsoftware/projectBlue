package com.lasthopesoftware.bluewater.settings.hidden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toAsync
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
				snapshotFlow { isUsingCustomCaching }.onEach { saveSettings().toAsync().await() }.launchIn(viewModelScope)
			}
	}

	fun saveSettings(): Promise<ApplicationSettings> = applicationSettingsRepository
		.promiseApplicationSettings()
		.eventually { settings ->
			settings.isUsingCustomCaching = isUsingCustomCaching
			applicationSettingsRepository.promiseUpdatedSettings(settings)
		}
}
