package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

public interface CheckIfAnyStoredItemsExist {
	Promise<Boolean> promiseIsAnyStoredItemsOrFiles(LibraryId libraryId);
}
