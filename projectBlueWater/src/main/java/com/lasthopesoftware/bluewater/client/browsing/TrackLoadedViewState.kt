package com.lasthopesoftware.bluewater.client.browsing

import com.lasthopesoftware.bluewater.shared.observables.InteractionState

interface TrackLoadedViewState {
	val isLoading: InteractionState<Boolean>
}
