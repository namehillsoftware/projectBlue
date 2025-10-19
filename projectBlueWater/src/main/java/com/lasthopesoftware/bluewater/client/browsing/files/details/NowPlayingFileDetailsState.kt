package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.shared.observables.InteractionState
import com.namehillsoftware.handoff.promises.Promise

interface NowPlayingFileDetailsState {
	val isInPosition: InteractionState<Boolean>
	fun removeFile(): Promise<Unit>
}
