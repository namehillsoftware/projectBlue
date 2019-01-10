package com.lasthopesoftware.bluewater.client.sync.library.items.conversion;

import com.lasthopesoftware.bluewater.client.sync.library.items.StoredItem;
import com.namehillsoftware.handoff.promises.Promise;

public interface ConvertStoredPlaylistsToStoredItems {
	Promise<StoredItem> promiseConvertedStoredItem(StoredItem storedItem);
}
