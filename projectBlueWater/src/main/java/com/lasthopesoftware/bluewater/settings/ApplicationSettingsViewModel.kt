package com.lasthopesoftware.bluewater.settings

import androidx.lifecycle.ViewModel
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
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApplicationSettingsViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	private val selectedPlaybackEngineTypeAccess: LookupSelectedPlaybackEngineType,
	private val libraryProvider: ILibraryProvider,
	receiveMessages: RegisterForApplicationMessages,
	private val syncScheduler: ScheduleSyncs,
) : ViewModel(), TrackLoadedViewState, ImmediateAction
{
	private val libraryChosenSubscription = receiveMessages.registerReceiver { m: BrowserLibrarySelection.LibraryChosenMessage ->
		mutableChosenLibraryId.value = m.chosenLibraryId
	}

	private val mutableLibraries = MutableStateFlow(emptyList<Library>())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableChosenLibraryId = MutableStateFlow(LibraryId(-1))
	private val mutableIsSyncOnPowerOnly = MutableStateFlow(false)
	private val mutableIsSyncOnWifiOnly = MutableStateFlow(false)
	private val mutableIsVolumeLevelingEnabled = MutableStateFlow(false)

	val isSyncOnWifiOnly = mutableIsSyncOnWifiOnly.asStateFlow()
	val isSyncOnPowerOnly = mutableIsSyncOnPowerOnly.asStateFlow()
	val isVolumeLevelingEnabled = mutableIsVolumeLevelingEnabled.asStateFlow()
	val playbackEngineType = MutableStateFlow(PlaybackEngineType.ExoPlayer)
	val chosenLibraryId = mutableChosenLibraryId.asStateFlow()
	val libraries = mutableLibraries.asStateFlow()
	override val isLoading = mutableIsLoading.asInteractionState()

	override fun onCleared() {
		libraryChosenSubscription.close()
	}

	fun loadSettings(): Promise<*> {
		mutableIsLoading.value = true

		val promisedSimpleValuesUpdate = applicationSettingsRepository
			.promiseApplicationSettings()
			.then { s ->
				mutableIsSyncOnWifiOnly.value = s.isSyncOnWifiOnly
				mutableIsSyncOnPowerOnly.value = s.isSyncOnPowerOnly
				mutableIsVolumeLevelingEnabled.value = s.isVolumeLevelingEnabled
				mutableChosenLibraryId.value = LibraryId(s.chosenLibraryId)
			}

		val promisedEngineTypeUpdate = selectedPlaybackEngineTypeAccess
			.promiseSelectedPlaybackEngineType()
			.then { it -> playbackEngineType.value = it }

		val promisedLibrariesUpdate = libraryProvider.allLibraries.then { it -> mutableLibraries.value = it.toList() }

		return Promise
			.whenAll(promisedSimpleValuesUpdate, promisedEngineTypeUpdate, promisedLibrariesUpdate)
			.must(this)
	}

	fun promiseSyncOnPowerChange(isSyncOnPowerOnly: Boolean): Promise<*> {
		mutableIsSyncOnPowerOnly.value = isSyncOnPowerOnly
		return saveSettings()
			.eventually {
				syncScheduler.scheduleSync()
			}
	}

	fun promiseSyncOnWifiChange(isSyncOnWifiOnly: Boolean): Promise<*> {
		mutableIsSyncOnWifiOnly.value = isSyncOnWifiOnly
		return saveSettings()
			.eventually {
				syncScheduler.scheduleSync()
			}
	}

	fun promiseVolumeLevelingEnabledChange(isVolumeLevelingEnabled: Boolean): Promise<*> {
		mutableIsVolumeLevelingEnabled.value = isVolumeLevelingEnabled
		return saveSettings()
	}

	private fun saveSettings(): Promise<*> =
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
