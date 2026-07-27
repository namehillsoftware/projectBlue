package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.InteractionState
import com.namehillsoftware.handoff.promises.Promise

interface LibraryListState {
	val libraries: InteractionState<List<Pair<LibraryId, String>>>
	fun loadLibraries(): Promise<Unit>
}
