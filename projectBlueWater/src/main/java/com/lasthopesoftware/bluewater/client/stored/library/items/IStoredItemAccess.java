package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface IStoredItemAccess {
	void toggleSync(IItem item, boolean enable);

	Promise<Boolean> isItemMarkedForSync(IItem item);

	Promise<Collection<StoredItem>> promiseStoredItems();
}
