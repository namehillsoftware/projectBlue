package com.lasthopesoftware.bluewater.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*

class ApplicationSettingsViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	private val selectedPlaybackEngineTypeAccess: LookupSelectedPlaybackEngineType,
	private val libraryProvider: ILibraryProvider,
	receiveMessages: RegisterForApplicationMessages,
	private val syncScheduler: ScheduleSyncs,
) : ViewModel(), TrackLoadedViewState, ImmediateAction
{
	private var isSyncOnPowerJob: Job? = null
	private var isSyncOnWifiJob: Job? = null
	private var isVolumeLevelingJob: Job? = null

	private val libraryChosenSubscription = receiveMessages.registerReceiver { m: BrowserLibrarySelection.LibraryChosenMessage ->
		chosenLibraryId.value = m.chosenLibraryId
	}

	private val mutableLibraries = MutableStateFlow(emptyList<Library>())
	private val mutableIsLoading = MutableStateFlow(false)

	val isSyncOnWifiOnly = MutableStateFlow(false)
	val isSyncOnPowerOnly = MutableStateFlow(false)
	val isVolumeLevelingEnabled = MutableStateFlow(false)
	val playbackEngineType = MutableStateFlow(PlaybackEngineType.ExoPlayer)
	val chosenLibraryId = MutableStateFlow(LibraryId(-1))
	val libraries = mutableLibraries.asStateFlow()
	override val isLoading = mutableIsLoading.asStateFlow()

	override fun onCleared() {
		libraryChosenSubscription.close()
		isSyncOnWifiJob?.cancel()
	}

	fun loadSettings(): Promise<*> {
		mutableIsLoading.value = true

		isSyncOnWifiJob?.cancel()
		isSyncOnPowerJob?.cancel()
		isVolumeLevelingJob?.cancel()

		val promisedSimpleValuesUpdate = applicationSettingsRepository
			.promiseApplicationSettings()
			.then { s ->
				isSyncOnWifiOnly.value = s.isSyncOnWifiOnly
				isSyncOnWifiJob = isSyncOnWifiOnly
					.dropWhile { it == s.isSyncOnWifiOnly }
					.onEach {
						saveSettings().suspend()
						syncScheduler.scheduleSync().suspend()
					}
					.launchIn(viewModelScope)

				isSyncOnPowerOnly.value = s.isSyncOnPowerOnly
				isSyncOnPowerJob = isSyncOnPowerOnly
					.dropWhile { it == s.isSyncOnPowerOnly }
					.onEach {
						saveSettings().suspend()
						syncScheduler.scheduleSync().suspend()
					}
					.launchIn(viewModelScope)

				isVolumeLevelingEnabled.value = s.isVolumeLevelingEnabled
				isVolumeLevelingJob = isVolumeLevelingEnabled
					.dropWhile { it == s.isVolumeLevelingEnabled }
					.onEach {
						saveSettings().suspend()
					}
					.launchIn(viewModelScope)

				chosenLibraryId.value = LibraryId(s.chosenLibraryId)
			}

		val promisedEngineTypeUpdate = selectedPlaybackEngineTypeAccess
			.promiseSelectedPlaybackEngineType()
			.then { playbackEngineType.value = it }

		val promisedLibrariesUpdate = libraryProvider.allLibraries.then { mutableLibraries.value = it.toList() }

		return Promise
			.whenAll(promisedSimpleValuesUpdate, promisedEngineTypeUpdate, promisedLibrariesUpdate)
			.must(this)
	}

	fun saveSettings(): Promise<*> =
		applicationSettingsRepository
			.promiseUpdatedSettings(
				ApplicationSettings(
					isSyncOnPowerOnly = isSyncOnPowerOnly.value,
					isVolumeLevelingEnabled = isVolumeLevelingEnabled.value,
					isSyncOnWifiOnly = isSyncOnWifiOnly.value,
					playbackEngineTypeName = playbackEngineType.value.name,
					chosenLibraryId = chosenLibraryId.value.id
				)
			)

	override fun act() {
		mutableIsLoading.value = false
	}
}
