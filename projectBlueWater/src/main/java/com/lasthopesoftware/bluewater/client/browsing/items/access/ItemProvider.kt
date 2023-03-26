package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.LibraryViewsProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemProvider(private val connectionProvider: ProvideLibraryConnections) : ProvideItems, ProvideFreshItems {
    override fun promiseItems(libraryId: LibraryId, itemId: ItemId?): Promise<List<Item>> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider
					?.run {
						itemId
							?.run { promiseResponse(LibraryViewsProvider.browseLibraryParameter, "ID=$id", "Version=2") }
							?: promiseResponse(LibraryViewsProvider.browseLibraryParameter, "Version=2")
					}
					.keepPromise()
			}
			.then { it?.body?.use { body -> body.byteStream().use(ItemResponse::getItems) } ?: emptyList() }
}
