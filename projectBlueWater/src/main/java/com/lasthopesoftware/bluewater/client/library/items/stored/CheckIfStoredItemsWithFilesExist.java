package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.namehillsoftware.handoff.promises.Promise;

public interface CheckIfStoredItemsWithFilesExist {
	Promise<Boolean> promiseIsAnyStoredItemsWithFiles();
}
