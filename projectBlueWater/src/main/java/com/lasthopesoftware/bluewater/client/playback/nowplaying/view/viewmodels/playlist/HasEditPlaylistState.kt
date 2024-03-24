package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist

import com.lasthopesoftware.bluewater.shared.observables.InteractionState

interface HasEditPlaylistState {
	val isEditingPlaylist : Boolean
	val isClearingPlaylistRequested: InteractionState<Boolean>
	val isClearingPlaylistRequestGranted: InteractionState<Boolean>
}
