package com.lasthopesoftware.bluewater.client.stored.library.items.specs;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemHelpers.getListType;

public class FakeStoredItemAccess implements IStoredItemAccess {

	private final List<StoredItem> inMemoryStoredItems = new ArrayList<>();

	public FakeStoredItemAccess(StoredItem... initialStoredItems) {
		inMemoryStoredItems.addAll(Arrays.asList(initialStoredItems));
	}

	@Override
	public void toggleSync(LibraryId libraryId, IItem item, boolean enable) {
		if (enable)
			inMemoryStoredItems.add(new StoredItem(1, item.getKey(), getListType(item)));
		else
			inMemoryStoredItems.removeAll(findMatchingItems(item));
	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(LibraryId libraryId, IItem item) {
		return new Promise<>(!findMatchingItems(item).isEmpty());
	}

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems(LibraryId libraryId) {
		return new Promise<>(inMemoryStoredItems);
	}

	private List<StoredItem> findMatchingItems(IItem item) {
		return Stream.of(inMemoryStoredItems).filter(i -> i.getServiceId() == item.getKey() && i.getItemType() == getListType(item)).toList();
	}

	@NotNull
	@Override
	public Promise<Object> disableAllLibraryItems(@NotNull LibraryId libraryId) {
		inMemoryStoredItems.removeAll(Stream.of(inMemoryStoredItems).filter(s -> s.getLibraryId() == libraryId.getId()).toList());
		return Promise.empty();
	}
}
