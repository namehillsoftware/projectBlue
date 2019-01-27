package com.lasthopesoftware.bluewater.client.stored.library.items.conversion;

import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.namehillsoftware.handoff.promises.Promise;

public interface ConvertStoredPlaylistsToStoredItems {
	Promise<StoredItem> promiseConvertedStoredItem(StoredItem storedItem);
}
