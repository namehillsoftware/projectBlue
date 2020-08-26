package com.lasthopesoftware.bluewater.client.stored.library.items.specs;

import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class FakeDeferredStoredItemAccess implements IStoredItemAccess {

	private Messenger<Collection<StoredItem>> messenger;

	public void resolveStoredItems() {
		if (messenger != null) {
			messenger.sendResolution(getStoredItems());
		}
	}

	protected abstract Collection<StoredItem> getStoredItems();

	@NotNull
	@Override
	public Promise<Object> disableAllLibraryItems(@NotNull LibraryId libraryId) {
		return Promise.empty();
	}

	@Override
	public void toggleSync(@NotNull LibraryId libraryId, @NotNull IItem item, boolean enable) {

	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(@NotNull LibraryId libraryId, @NotNull IItem item) {
		return new Promise<>(false);
	}

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems(@NotNull LibraryId libraryId) {
		return new Promise<>((m -> messenger = m));
	}
}
