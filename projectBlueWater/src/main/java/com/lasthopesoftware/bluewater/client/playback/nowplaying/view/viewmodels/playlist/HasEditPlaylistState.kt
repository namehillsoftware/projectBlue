package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist

import com.lasthopesoftware.observables.InteractionState

interface HasEditPlaylistState {
	val isEditingPlaylist: InteractionState<Boolean>
	val isClearingPlaylistRequested: InteractionState<Boolean>
	val isClearingPlaylistRequestGranted: InteractionState<Boolean>
}
