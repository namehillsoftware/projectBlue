package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.LibraryViewsProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise

class ItemProvider(private val connectionProvider: ProvideLibraryConnections) : ProvideItems, ProvideFreshItems {
    override fun promiseItems(libraryId: LibraryId, item: Item): Promise<List<Item>> =
    	connectionProvider
			.promiseLibraryConnection(libraryId)
			.eventually { c -> c?.promiseResponse(LibraryViewsProvider.browseLibraryParameter, "ID=${item.key}", "Version=2") ?: Promise.empty() }
			.then { it?.body?.use { body -> body.byteStream().use(ItemResponse::GetItems) } ?: emptyList() }
}
