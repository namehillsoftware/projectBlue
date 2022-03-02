package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import kotlinx.coroutines.flow.StateFlow

interface ControlPlaylistEdits {
	fun isEditingPlaylist(): StateFlow<Boolean>
}
