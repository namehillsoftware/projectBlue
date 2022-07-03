package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class StateChangeBroadcastingStoredItemAccess(private val inner: AccessStoredItems, private val sendApplicationMessages: SendApplicationMessages) : AccessStoredItems by inner {
	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit> =
		inner.toggleSync(libraryId, item, enable)
			.then { sendApplicationMessages.sendMessage(SyncItemStateChanged) }

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> =
		inner
			.toggleSync(libraryId, itemId, enable)
			.then { sendApplicationMessages.sendMessage(SyncItemStateChanged) }
}
