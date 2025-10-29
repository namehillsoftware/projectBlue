package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemProvider(private val connectionProvider: ProvideGuaranteedLibraryConnections) :
	ProvideItems,
	ProvideFreshItems
{
    override fun promiseItems(libraryId: LibraryId, itemId: KeyedIdentifier?): Promise<List<IItem>> = Promise.Proxy { cp ->
		connectionProvider
			.promiseLibraryAccess(libraryId)
			.also(cp::doCancel)
			.eventually { access ->
				access
					?.promiseItems(itemId)
					.keepPromise { emptyList() }
			}
	}
}
