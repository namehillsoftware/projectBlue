package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

private const val browseLibraryParameter = "Browse/Children"

class ItemProvider(private val connectionProvider: ProvideLibraryConnections) : ProvideItems, ProvideFreshItems {
    override fun promiseItems(libraryId: LibraryId, itemId: ItemId?): Promise<List<Item>> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider
					?.run {
						itemId
							?.run { promiseResponse(browseLibraryParameter, "ID=$id", "Version=2", "ErrorOnMissing=1") }
							?: promiseResponse(browseLibraryParameter, "Version=2", "ErrorOnMissing=1")
					}
					.keepPromise()
			}
			.then { it?.body?.use { body -> body.byteStream().use(ItemResponse::getItems) } ?: emptyList() }
}
