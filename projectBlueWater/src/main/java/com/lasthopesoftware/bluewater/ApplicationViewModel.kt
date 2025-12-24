package com.lasthopesoftware.bluewater

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsUpdated
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class ApplicationViewModel(
	private val applicationSettingsRepository: HoldApplicationSettings,
	messageBus: RegisterForApplicationMessages,
) : ViewModel() {

	init {
		addCloseable(messageBus.registerReceiver { _: ApplicationSettingsUpdated ->
			loadSettings()
		})
	}

	private val mutableTheme = MutableInteractionState(ApplicationSettings.Theme.SYSTEM)
	val theme = mutableTheme.asInteractionState()

	fun loadSettings(): Promise<*> =
		applicationSettingsRepository
			.promiseApplicationSettings()
			.then { settings ->
				mutableTheme.value = settings.theme ?: ApplicationSettings.Theme.SYSTEM
			}
}
