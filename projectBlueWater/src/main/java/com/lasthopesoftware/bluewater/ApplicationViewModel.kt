package com.lasthopesoftware.bluewater

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsUpdated
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

data class ApplicationState(val isTv: Boolean)

interface AccessActivityState {
	fun promiseApplicationState(): Promise<ApplicationState>
}

class ApplicationViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	private val applicationStateAccess: AccessActivityState,
	messageBus: RegisterForApplicationMessages,
) : ViewModel(), ManageApplicationPropertyState {

	init {
		addCloseable(messageBus.registerReceiver { _: ApplicationSettingsUpdated ->
			loadSettings()
		})
	}

	private val mutableIsTv = MutableInteractionState(false)
	private val mutableTheme = MutableInteractionState(ApplicationSettings.Theme.SYSTEM)

	override val isTv = mutableIsTv.asInteractionState()
	override val theme = mutableTheme.asInteractionState()

	override fun loadSettings(): Promise<*> {
		val promisedApplicationState = applicationStateAccess.promiseApplicationState()
		return applicationSettingsRepository
			.promiseApplicationSettings()
			.eventually { settings ->
				mutableTheme.value = settings.theme ?: ApplicationSettings.Theme.SYSTEM
				promisedApplicationState.then { state ->
					mutableIsTv.value = state.isTv
				}
			}
	}
}
