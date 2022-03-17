package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredItemsChecker implements CheckIfAnyStoredItemsExist {
	private final AccessStoredItems storedItemAccess;
	private final CheckForAnyStoredFiles checkForAnyStoredFiles;

	public StoredItemsChecker(AccessStoredItems storedItemAccess, CheckForAnyStoredFiles checkForAnyStoredFiles) {
		this.storedItemAccess = storedItemAccess;
		this.checkForAnyStoredFiles = checkForAnyStoredFiles;
	}

	@Override
	public Promise<Boolean> promiseIsAnyStoredItemsOrFiles(LibraryId libraryId) {
		return storedItemAccess.promiseStoredItems(libraryId)
			.eventually(items -> !items.isEmpty()
				? new Promise<>(true)
				: checkForAnyStoredFiles.promiseIsAnyStoredFiles(libraryId));
	}
}
