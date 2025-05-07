package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist

import com.namehillsoftware.handoff.promises.Promise

interface ControlPlaylistEdits {
	fun editPlaylist()
	fun finishPlaylistEdit(): Boolean
	fun requestPlaylistClearingPermission()
	fun grantPlaylistClearing()
	fun clearPlaylistIfGranted(): Promise<*>
}
