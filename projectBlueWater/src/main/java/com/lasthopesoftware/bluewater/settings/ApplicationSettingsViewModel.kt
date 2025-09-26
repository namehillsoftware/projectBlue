package com.lasthopesoftware.bluewater.settings

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.LookupLibraryName
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
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

class ApplicationSettingsViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	private val selectedPlaybackEngineTypeAccess: LookupSelectedPlaybackEngineType,
	private val librarySettingsProvider: ProvideLibrarySettings,
	private val libraryNameLookup: LookupLibraryName,
	receiveMessages: RegisterForApplicationMessages,
	private val syncScheduler: ScheduleSyncs,
) : ViewModel(), TrackLoadedViewState, ImmediateAction
{
	private val libraryChosenSubscription = receiveMessages.registerReceiver { m: BrowserLibrarySelection.LibraryChosenMessage ->
		mutableChosenLibraryId.value = m.chosenLibraryId
	}

	private val mutableLibraries = MutableInteractionState(emptyList<Pair<LibraryId, String>>())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableChosenLibraryId = MutableInteractionState(LibraryId(-1))
	private val mutableIsSyncOnPowerOnly = MutableInteractionState(false)
	private val mutableIsSyncOnWifiOnly = MutableInteractionState(false)
	private val mutableIsVolumeLevelingEnabled = MutableInteractionState(false)
	private val mutableIsPeakLevelNormalizeEnabled = MutableInteractionState(false)

	val isSyncOnWifiOnly = mutableIsSyncOnWifiOnly.asInteractionState()
	val isSyncOnPowerOnly = mutableIsSyncOnPowerOnly.asInteractionState()
	val isVolumeLevelingEnabled = mutableIsVolumeLevelingEnabled.asInteractionState()
	val isPeakLevelNormalizeEditable = isVolumeLevelingEnabled
	val isPeakLevelNormalizeEnabled = mutableIsPeakLevelNormalizeEnabled.asInteractionState()
	val playbackEngineType = MutableInteractionState(PlaybackEngineType.ExoPlayer)
	val chosenLibraryId = mutableChosenLibraryId.asInteractionState()
	val libraries = mutableLibraries.asInteractionState()
	override val isLoading = mutableIsLoading.asInteractionState()

	override fun onCleared() {
		libraryChosenSubscription.close()
	}

	override fun act() {
		mutableIsLoading.value = false
	}

	fun loadSettings(): Promise<*> {
		mutableIsLoading.value = true

		val promisedSimpleValuesUpdate = applicationSettingsRepository
			.promiseApplicationSettings()
			.then { s ->
				mutableIsSyncOnWifiOnly.value = s.isSyncOnWifiOnly
				mutableIsSyncOnPowerOnly.value = s.isSyncOnPowerOnly
				mutableIsVolumeLevelingEnabled.value = s.isVolumeLevelingEnabled
				mutableIsPeakLevelNormalizeEnabled.value = s.isPeakLevelNormalizeEnabled
				mutableChosenLibraryId.value = LibraryId(s.chosenLibraryId)
			}

		val promisedEngineTypeUpdate = selectedPlaybackEngineTypeAccess
			.promiseSelectedPlaybackEngineType()
			.then { it -> playbackEngineType.value = it }

		val promisedLibrariesUpdate = librarySettingsProvider
			.promiseAllLibrarySettings()
			.eventually {
				Promise.whenAll(
					it.mapNotNull { l ->
						l.libraryId
							?.let { libraryId ->
								libraryNameLookup
									.promiseLibraryName(libraryId)
									.then { n -> Pair(libraryId, n ?: "") }
							}
					}
				)
			}
			.then { it -> mutableLibraries.value = it.sortedBy { it.first.id } }

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

	fun promisePeakLevelNormalizeEnabledChange(isPeakLevelNormalizeEnabled: Boolean): Promise<*> {
		mutableIsPeakLevelNormalizeEnabled.value = isPeakLevelNormalizeEnabled
		return saveSettings()
	}

	private fun saveSettings(): Promise<*> =
		applicationSettingsRepository
			.promiseUpdatedSettings(
				ApplicationSettings(
					isSyncOnPowerOnly = isSyncOnPowerOnly.value,
					isVolumeLevelingEnabled = isVolumeLevelingEnabled.value,
					isPeakLevelNormalizeEnabled = isPeakLevelNormalizeEnabled.value,
					isSyncOnWifiOnly = isSyncOnWifiOnly.value,
					playbackEngineTypeName = playbackEngineType.value.name,
					chosenLibraryId = chosenLibraryId.value.id
				)
			)
}
