package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface IStoredItemAccess {
	void toggleSync(LibraryId libraryId, IItem item, boolean enable);

	Promise<Boolean> isItemMarkedForSync(LibraryId libraryId, IItem item);

	Promise<Collection<StoredItem>> promiseStoredItems(LibraryId libraryId);
}
