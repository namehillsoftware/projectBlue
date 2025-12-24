package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import com.lasthopesoftware.observables.InteractionState

interface ControlScreenOnState {
	val isScreenOnEnabled: InteractionState<Boolean>
	fun toggleScreenOn()
}
