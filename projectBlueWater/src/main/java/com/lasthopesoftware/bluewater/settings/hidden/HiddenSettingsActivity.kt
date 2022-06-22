package com.lasthopesoftware.bluewater.settings.hidden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
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
	val usingCustomCaching by hiddenSettingsViewModel.isUsingCustomCaching.collectAsState()

	Column {
		Row {
			Column {
				Checkbox(
					checked = usingCustomCaching,
					enabled = true,
					modifier = Modifier.padding(16.dp),
					onCheckedChange = { hiddenSettingsViewModel.isUsingCustomCaching.value = it }
				)
			}

			Column {
				Text(
					text = "Use custom caching implementation",
					modifier = Modifier.padding(16.dp),
				)
			}
		}
	}
}

class HiddenSettingsViewModel(private val applicationSettingsRepository: HoldApplicationSettings) : ViewModel() {
	var isUsingCustomCaching = MutableStateFlow(false)

	init {
	    applicationSettingsRepository
			.promiseApplicationSettings()
			.then { settings ->
				isUsingCustomCaching.value = settings.isUsingCustomCaching
				isUsingCustomCaching.drop(1).onEach { saveSettings().suspend() }.launchIn(viewModelScope)
			}
	}

	private fun saveSettings(): Promise<ApplicationSettings> = applicationSettingsRepository
		.promiseApplicationSettings()
		.eventually { settings ->
			settings.isUsingCustomCaching = isUsingCustomCaching.value
			applicationSettingsRepository.promiseUpdatedSettings(settings)
		}
}
