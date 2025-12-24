package com.lasthopesoftware.bluewater.client.browsing

import com.lasthopesoftware.observables.InteractionState

interface TrackLoadedViewState {
	val isLoading: InteractionState<Boolean>
}
