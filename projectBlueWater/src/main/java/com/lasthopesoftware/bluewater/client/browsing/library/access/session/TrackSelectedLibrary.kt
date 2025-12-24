package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.InteractionState
import com.namehillsoftware.handoff.promises.Promise

interface TrackSelectedLibrary {
	val selectedLibraryId: InteractionState<LibraryId?>
	fun loadSelectedLibraryId(): Promise<LibraryId?>
	fun selectLibrary(libraryId: LibraryId): Promise<LibraryId>
}
