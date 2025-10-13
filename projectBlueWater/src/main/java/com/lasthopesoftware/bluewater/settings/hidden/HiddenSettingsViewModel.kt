package com.lasthopesoftware.bluewater.settings.hidden

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.connection.okhttp.HttpClientType
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class HiddenSettingsViewModel(private val applicationSettingsRepository: HoldApplicationSettings) : ViewModel(),
    TrackLoadedViewState {

	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsLoggingToFile = MutableInteractionState(false)
	private val mutableHttpClientType = MutableInteractionState(HttpClientType.OkHttp)

	val isLoggingToFile = mutableIsLoggingToFile.asInteractionState()
	val httpClientType = mutableHttpClientType.asInteractionState()

	override val isLoading = mutableIsLoading.asInteractionState()

	fun loadApplicationSettings(): Promise<Unit> {
		mutableIsLoading.value = true
		return applicationSettingsRepository
			.promiseApplicationSettings()
			.then { it ->
				mutableIsLoggingToFile.value = it.isLoggingToFile
				mutableHttpClientType.value = it.httpClientTypeName?.let(HttpClientType::valueOf) ?: HttpClientType.OkHttp
			}
			.must { _ ->
				mutableIsLoading.value = false
			}
	}

	fun promiseIsLoggingToFile(isLoggingToFile: Boolean): Promise<ApplicationSettings> {
		mutableIsLoggingToFile.value = isLoggingToFile
		return saveSettings()
	}

	fun promiseHttpClientType(httpClientType: HttpClientType): Promise<ApplicationSettings> {
		mutableHttpClientType.value = httpClientType
		return saveSettings()
	}

    private fun saveSettings(): Promise<ApplicationSettings> {
		mutableIsLoading.value = true
		return applicationSettingsRepository
			.promiseApplicationSettings()
			.eventually { settings ->
				settings.isLoggingToFile = isLoggingToFile.value
				settings.httpClientTypeName = httpClientType.value.name
				applicationSettingsRepository.promiseUpdatedSettings(settings)
			}
			.must { _ ->
				mutableIsLoading.value = false
			}
	}
}
