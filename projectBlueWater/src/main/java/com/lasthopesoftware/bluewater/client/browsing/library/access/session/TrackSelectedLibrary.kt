package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.InteractionState

interface TrackSelectedLibrary : ProvideSelectedLibraryId, SelectBrowserLibrary {
	val selectedLibraryId: InteractionState<LibraryId?>
}
