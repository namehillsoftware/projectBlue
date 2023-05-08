package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import kotlinx.coroutines.flow.StateFlow

interface ControlScreenOnState {
	val isScreenOnEnabled: StateFlow<Boolean>
	fun toggleScreenOn()
}
