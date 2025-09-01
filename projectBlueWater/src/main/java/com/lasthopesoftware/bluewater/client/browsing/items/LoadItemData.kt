package com.lasthopesoftware.bluewater.client.browsing.items

import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface LoadItemData : TrackLoadedViewState {
	fun loadItem(libraryId: LibraryId, item: IItem? = null): Promise<Unit>
	fun promiseRefresh(): Promise<Unit>
}
