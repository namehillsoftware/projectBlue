package com.lasthopesoftware.bluewater.client.library.items.stored.conversion;

import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.namehillsoftware.handoff.promises.Promise;

public interface ConvertStoredPlaylistsToStoredItems {
	Promise<StoredItem> promiseConvertedStoredItem(StoredItem storedItem);
}
