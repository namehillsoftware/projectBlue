package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemStringListProvider(
	private val remoteLibraryAccess: ProvideRemoteLibraryAccess
) : ProvideFileStringListForItem {
	override fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId?, options: FileListParameters.Options): Promise<String> {
		// Put any crazy workarounds to get a fresh file list in here
		return remoteLibraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { a -> a?.promiseFileStringList(itemId).keepPromise("") }
	}
}
