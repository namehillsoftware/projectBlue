package com.lasthopesoftware.bluewater.client.library.items.stored.specs;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemHelpers.getListType;

public class FakeStoredItemAccess implements IStoredItemAccess {

	private final List<StoredItem> inMemoryStoredItems = new ArrayList<>();

	public FakeStoredItemAccess(StoredItem... initialStoredItems) {
		inMemoryStoredItems.addAll(Arrays.asList(initialStoredItems));
	}

	@Override
	public void toggleSync(IItem item, boolean enable) {
		if (enable)
			inMemoryStoredItems.add(new StoredItem(1, item.getKey(), getListType(item)));
		else
			inMemoryStoredItems.removeAll(findMatchingItems(item));
	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(IItem item) {
		return new Promise<>(!findMatchingItems(item).isEmpty());
	}

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems() {
		return new Promise<>(inMemoryStoredItems);
	}

	private List<StoredItem> findMatchingItems(IItem item) {
		return Stream.of(inMemoryStoredItems).filter(i -> i.getServiceId() == item.getKey() && i.getItemType() == getListType(item)).toList();
	}
}
