package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.namehillsoftware.handoff.promises.Promise;

public class StoredItemsChecker implements CheckIfStoredItemsWithFilesExist {
	private final IStoredItemAccess storedItemAccess;

	public StoredItemsChecker(IStoredItemAccess storedItemAccess) {
		this.storedItemAccess = storedItemAccess;
	}

	@Override
	public Promise<Boolean> promiseIsAnyStoredItemsWithFiles() {
		return storedItemAccess.promiseStoredItems()
			.then(items -> !items.isEmpty());
	}
}
