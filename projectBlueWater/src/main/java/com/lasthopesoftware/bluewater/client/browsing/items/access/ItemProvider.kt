package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.LibraryViewsProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ItemProvider(private val connectionProvider: IConnectionProvider) : ProvideItems {

	companion object {
		@JvmStatic
		fun provide(connectionProvider: IConnectionProvider, itemKey: Int): Promise<List<Item>> {
			return ItemProvider(connectionProvider).promiseItems(itemKey)
		}
	}

    override fun promiseItems(itemKey: Int): Promise<List<Item>> =
    	connectionProvider
			.promiseResponse(LibraryViewsProvider.browseLibraryParameter, "ID=$itemKey", "Version=2")
			.then { it.body?.use { body -> body.byteStream().use(ItemResponse::GetItems) } }
}
