package com.lasthopesoftware.bluewater.settings.hidden

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class HiddenSettingsViewModel(private val applicationSettingsRepository: HoldApplicationSettings) : ViewModel(),
    TrackLoadedViewState {

	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsLoggingToFile = MutableInteractionState(false)

	val isLoggingToFile = mutableIsLoggingToFile.asInteractionState()

	override val isLoading = mutableIsLoading.asInteractionState()

	fun loadApplicationSettings(): Promise<Unit> {
		mutableIsLoading.value = true
		return applicationSettingsRepository
			.promiseApplicationSettings()
			.then {
				mutableIsLoggingToFile.value = it.isLoggingToFile
			}
			.must {
				mutableIsLoading.value = false
			}
	}

	fun promiseIsLoggingToFile(isLoggingToFile: Boolean): Promise<ApplicationSettings> {
		mutableIsLoggingToFile.value = isLoggingToFile
		return saveSettings()
	}

    private fun saveSettings(): Promise<ApplicationSettings> {
		mutableIsLoading.value = true
		return applicationSettingsRepository
			.promiseApplicationSettings()
			.eventually { settings ->
				settings.isLoggingToFile = isLoggingToFile.value
				applicationSettingsRepository.promiseUpdatedSettings(settings)
			}
			.must {
				mutableIsLoading.value = false
			}
	}
}
