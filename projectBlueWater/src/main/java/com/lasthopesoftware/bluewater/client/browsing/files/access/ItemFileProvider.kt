package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ItemFileProvider(private val fileStringListProvider: ProvideFileStringListForItem) : ProvideItemFiles {
	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId?): Promise<List<ServiceFile>> =
		fileStringListProvider
			.promiseFileStringList(libraryId, itemId)
			.eventually(FileResponses)
			.then(FileResponses)
}
