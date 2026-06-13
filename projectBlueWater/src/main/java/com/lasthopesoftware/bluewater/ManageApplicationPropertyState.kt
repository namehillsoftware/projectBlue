package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.observables.InteractionState
import com.namehillsoftware.handoff.promises.Promise

interface ManageApplicationPropertyState {
	val theme: InteractionState<ApplicationSettings.Theme>
	val isTv: InteractionState<Boolean>
	fun loadSettings(): Promise<*>
}
