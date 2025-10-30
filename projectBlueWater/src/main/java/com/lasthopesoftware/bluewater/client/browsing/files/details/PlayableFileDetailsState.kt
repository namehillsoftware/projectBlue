package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.shared.observables.InteractionState

interface PlayableFileDetailsState {
	fun play()
    val isPlayableWithPlaylist: InteractionState<Boolean>
}
