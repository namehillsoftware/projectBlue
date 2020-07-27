package com.lasthopesoftware.bluewater.client.stored.library.items.conversion

import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.namehillsoftware.handoff.promises.Promise

interface ConvertStoredPlaylistsToStoredItems {
	fun promiseConvertedStoredItem(storedItem: StoredItem): Promise<StoredItem>
}
