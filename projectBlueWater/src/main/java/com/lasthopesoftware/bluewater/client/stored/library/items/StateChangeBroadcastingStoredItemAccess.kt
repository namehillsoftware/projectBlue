package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.itemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class StateChangeBroadcastingStoredItemAccess(private val inner: AccessStoredItems, private val sendApplicationMessages: SendApplicationMessages) : AccessStoredItems by inner {
	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> =
		inner
			.toggleSync(libraryId, itemId)
			.then { isSynced ->
				sendApplicationMessages.sendMessage(SyncItemStateChanged(libraryId, itemId, isSynced))
				isSynced
			}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit> =
		inner
			.toggleSync(libraryId, item, enable)
			.then { _ ->
				sendApplicationMessages.sendMessage(
					SyncItemStateChanged(libraryId, item.itemId, enable)
				)
			}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> =
		inner
			.toggleSync(libraryId, itemId, enable)
			.then { _ -> sendApplicationMessages.sendMessage(SyncItemStateChanged(libraryId, itemId, enable)) }
}
