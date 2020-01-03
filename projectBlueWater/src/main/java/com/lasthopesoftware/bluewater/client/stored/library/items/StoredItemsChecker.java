package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredItemsChecker implements CheckIfAnyStoredItemsExist {
	private final IStoredItemAccess storedItemAccess;
	private final CheckForAnyStoredFiles checkForAnyStoredFiles;

	public StoredItemsChecker(IStoredItemAccess storedItemAccess, CheckForAnyStoredFiles checkForAnyStoredFiles) {
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
