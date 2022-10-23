package com.lasthopesoftware.bluewater.client.browsing

import kotlinx.coroutines.flow.StateFlow

interface TrackLoadedViewState {
	val isLoading: StateFlow<Boolean>
}
