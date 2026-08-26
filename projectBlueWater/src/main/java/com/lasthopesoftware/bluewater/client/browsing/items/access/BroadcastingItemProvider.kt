package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

data class ItemUpdated(val libraryId: LibraryId, val item: IItem) : ApplicationMessage

class BroadcastingItemProvider(
	private val inner: ProvideItems,
	private val applicationMessageBus: SendApplicationMessages,
) : ProvideItems {
	override fun promiseItems(libraryId: LibraryId, itemId: KeyedIdentifier?): Promise<List<IItem>> =
		inner
			.promiseItems(libraryId, itemId)
			.then { items ->
				for (item in items) {
					applicationMessageBus.sendMessage(ItemUpdated(libraryId, item))
				}

				items
			}
}
