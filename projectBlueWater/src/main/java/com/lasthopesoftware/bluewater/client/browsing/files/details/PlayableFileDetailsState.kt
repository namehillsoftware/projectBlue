package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.observables.InteractionState

interface PlayableFileDetailsState {
	fun play()
    val isPlayableWithPlaylist: InteractionState<Boolean>
}
