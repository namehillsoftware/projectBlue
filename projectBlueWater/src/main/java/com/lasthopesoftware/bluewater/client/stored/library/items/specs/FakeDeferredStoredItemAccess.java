package com.lasthopesoftware.bluewater.client.stored.library.items.specs;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public abstract class FakeDeferredStoredItemAccess implements IStoredItemAccess {

	private Messenger<Collection<StoredItem>> messenger;

	public void resolveStoredItems() {
		if (messenger != null) {
			messenger.sendResolution(getStoredItems());
		}
	}

	protected abstract Collection<StoredItem> getStoredItems();

	@Override
	public void toggleSync(LibraryId libraryId, IItem item, boolean enable) {

	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(LibraryId libraryId, IItem item) {
		return new Promise<>(false);
	}

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems(LibraryId libraryId) {
		return new Promise<>((m -> messenger = m));
	}
}
