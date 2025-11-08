package com.lasthopesoftware.bluewater.settings.hidden

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.connection.http.HttpClientType
import com.lasthopesoftware.bluewater.client.playback.exoplayer.HttpDataSourceType
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class HiddenSettingsViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
) : ViewModel(),
    TrackLoadedViewState {

	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsLoggingToFile = MutableInteractionState(false)
	private val mutableDataSourceType = MutableInteractionState(HttpDataSourceType.OkHttp)
	private val mutableHttpClientType = MutableInteractionState(HttpClientType.OkHttp)

	val isLoggingToFile = mutableIsLoggingToFile.asInteractionState()
	val dataSourceType = mutableDataSourceType.asInteractionState()
	val httpClientType = mutableHttpClientType.asInteractionState()

	override val isLoading = mutableIsLoading.asInteractionState()

	fun loadApplicationSettings(): Promise<Unit> {
		mutableIsLoading.value = true
		val promisedApplicationFeatures = applicationFeatureConfiguration
			.promiseFeatureConfiguration()
			.then { f ->
				mutableDataSourceType.value = f.httpDataSourceType ?: HttpDataSourceType.OkHttp
				mutableHttpClientType.value = f.httpClientType ?: HttpClientType.OkHttp
			}

		val promisedApplicationSettingsUpdate = applicationSettingsRepository
			.promiseApplicationSettings()
			.then {
				mutableIsLoggingToFile.value = it.isLoggingToFile
			}

		return Promise.whenAll(promisedApplicationSettingsUpdate, promisedApplicationFeatures)
			.unitResponse()
			.must { _ ->
				mutableIsLoading.value = false
			}
	}

	fun promiseIsLoggingToFile(isLoggingToFile: Boolean): Promise<*> {
		mutableIsLoggingToFile.value = isLoggingToFile
		return saveSettings()
	}

	fun promiseDataSourceUpdate(dataSourceType: HttpDataSourceType): Promise<*> {
		mutableDataSourceType.value = dataSourceType
		return saveSettings()
	}

	fun promiseHttpClientType(httpClientType: HttpClientType): Promise<*> {
		mutableHttpClientType.value = httpClientType
		return saveSettings()
	}

    private fun saveSettings(): Promise<*> {
		mutableIsLoading.value = true

		val promisedSettingsUpdate = applicationSettingsRepository
			.promiseApplicationSettings()
			.eventually { settings ->
				settings.isLoggingToFile = isLoggingToFile.value
				applicationSettingsRepository.promiseUpdatedSettings(settings)
			}
			.unitResponse()

		val promisedFeaturesUpdate = applicationFeatureConfiguration
			.promiseFeatureConfiguration()
			.eventually { features ->
				val newFeatures = features.copy(httpDataSourceType = mutableDataSourceType.value, httpClientType = mutableHttpClientType.value)
				applicationFeatureConfiguration.promiseUpdatedFeatureConfiguration(newFeatures)
			}
			.unitResponse()

		return Promise.whenAll(promisedSettingsUpdate, promisedFeaturesUpdate)
			.unitResponse()
			.must { _ ->
				mutableIsLoading.value = false
			}
	}
}
