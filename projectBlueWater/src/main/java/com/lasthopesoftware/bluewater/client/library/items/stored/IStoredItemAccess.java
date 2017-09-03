package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.List;

public interface IStoredItemAccess {
	void toggleSync(IItem item, boolean enable);

	Promise<Boolean> isItemMarkedForSync(IItem item);

	Promise<List<StoredItem>> getStoredItems();
}
