package com.lasthopesoftware.bluewater.settings

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApplicationSettingsViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	private val selectedPlaybackEngineTypeAccess: LookupSelectedPlaybackEngineType,
	private val libraryProvider: ILibraryProvider,
) : ViewModel(), TrackLoadedViewState, ImmediateAction
{
	private val mutableLibraries = MutableStateFlow(emptyList<Library>())
	private val mutableIsLoading = MutableStateFlow(false)

	val isSyncOnWifiOnly = MutableStateFlow(false)
	val isSyncOnPowerOnly = MutableStateFlow(false)
	val isVolumeLevelingEnabled = MutableStateFlow(false)
	val playbackEngineType = MutableStateFlow(PlaybackEngineType.ExoPlayer)
	val chosenLibraryId = MutableStateFlow(LibraryId(-1))
	val libraries = mutableLibraries.asStateFlow()
	override val isLoading = mutableIsLoading.asStateFlow()

	fun loadSettings(): Promise<*> {
		mutableIsLoading.value = true

		val promisedSimpleValuesUpdate = applicationSettingsRepository.promiseApplicationSettings()
			.then {
				isSyncOnWifiOnly.value = it.isSyncOnWifiOnly
				isSyncOnPowerOnly.value = it.isSyncOnPowerOnly
				isVolumeLevelingEnabled.value = it.isVolumeLevelingEnabled
				chosenLibraryId.value = LibraryId(it.chosenLibraryId)
			}

		val promisedEngineTypeUpdate = selectedPlaybackEngineTypeAccess
			.promiseSelectedPlaybackEngineType()
			.then { playbackEngineType.value = it }

		val promisedLibrariesUpdate = libraryProvider.allLibraries.then { mutableLibraries.value = it.toList() }

		return Promise
			.whenAll(promisedSimpleValuesUpdate, promisedEngineTypeUpdate, promisedLibrariesUpdate)
			.must(this)
	}

	fun addServer() {

	}

	fun saveSettings(): Promise<*> = Unit.toPromise()

	override fun act() {
		mutableIsLoading.value = false
	}
}
