package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemStringListProvider(
	private val libraryConnections: ProvideLibraryConnections
) : ProvideFileStringListForItem {
	override fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId?, options: FileListParameters.Options): Promise<String> {
		// Put any crazy workarounds to get a fresh file list in here
		return libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { a -> a?.promiseFileStringList(itemId).keepPromise("") }
	}
}
