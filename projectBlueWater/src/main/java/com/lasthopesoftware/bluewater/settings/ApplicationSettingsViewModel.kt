package com.lasthopesoftware.bluewater.settings

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.LookupLibraryName
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
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
	private val librarySettingsProvider: ProvideLibrarySettings,
	private val libraryNameLookup: LookupLibraryName,
	receiveMessages: RegisterForApplicationMessages,
	private val syncScheduler: ScheduleSyncs,
) : ViewModel(), TrackLoadedViewState, ImmediateAction
{
	enum class SelectedTab {
		ViewServers, ViewSettings
	}

	private val mutableLibraries = MutableInteractionState(emptyList<Pair<LibraryId, String>>())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableChosenLibraryId = MutableInteractionState<LibraryId?>(null)
	private val mutableIsSyncOnPowerOnly = MutableInteractionState(false)
	private val mutableIsSyncOnWifiOnly = MutableInteractionState(false)
	private val mutableIsVolumeLevelingEnabled = MutableInteractionState(false)
	private val mutableIsPeakLevelNormalizeEnabled = MutableInteractionState(false)
	private val mutableTheme = MutableInteractionState(ApplicationSettings.Theme.SYSTEM)

	val isSyncOnWifiOnly = mutableIsSyncOnWifiOnly.asInteractionState()
	val isSyncOnPowerOnly = mutableIsSyncOnPowerOnly.asInteractionState()
	val isVolumeLevelingEnabled = mutableIsVolumeLevelingEnabled.asInteractionState()
	val isPeakLevelNormalizeEditable = isVolumeLevelingEnabled
	val isPeakLevelNormalizeEnabled = mutableIsPeakLevelNormalizeEnabled.asInteractionState()
	val chosenLibraryId = mutableChosenLibraryId.asInteractionState()
	val libraries = mutableLibraries.asInteractionState()
	val theme = mutableTheme.asInteractionState()
	val selectedTab = MutableInteractionState(SelectedTab.ViewServers)
	override val isLoading = mutableIsLoading.asInteractionState()

	init {
	    addCloseable(receiveMessages.registerReceiver { m: BrowserLibrarySelection.LibraryChosenMessage ->
			mutableChosenLibraryId.value = m.chosenLibraryId
		})
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
				mutableTheme.value = s.theme ?: ApplicationSettings.Theme.SYSTEM
				mutableChosenLibraryId.value = s.chosenLibraryId.takeIf { it > -1 }?.let(::LibraryId)
			}

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
			.whenAll(promisedSimpleValuesUpdate, promisedLibrariesUpdate)
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

	fun promiseThemeChange(theme: ApplicationSettings.Theme): Promise<*> {
		mutableTheme.value = theme
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
					chosenLibraryId = chosenLibraryId.value?.id ?: -1,
					theme = theme.value
				)
			)
}
