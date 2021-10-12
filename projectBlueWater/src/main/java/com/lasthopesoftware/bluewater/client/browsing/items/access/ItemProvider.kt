package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.LibraryViewsProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ItemProvider(private val connectionProvider: IConnectionProvider) : ProvideItems {
    override fun promiseItems(libraryId: LibraryId, itemKey: Int): Promise<List<Item>> =
    	connectionProvider
			.promiseResponse(LibraryViewsProvider.browseLibraryParameter, "ID=$itemKey", "Version=2")
			.then { it.body?.use { body -> body.byteStream().use(ItemResponse::GetItems) } ?: emptyList() }
}
