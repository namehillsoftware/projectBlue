package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.shared.observables.InteractionState

interface NowPlayingFileDetailsState {
	val isInPosition: InteractionState<Boolean>
	fun removeFile()
}
