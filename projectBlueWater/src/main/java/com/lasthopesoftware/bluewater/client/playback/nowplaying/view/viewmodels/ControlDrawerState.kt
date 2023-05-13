package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

interface ControlDrawerState {
	fun showDrawer()
	fun hideDrawer()
	val isDrawerShown: Boolean
}
