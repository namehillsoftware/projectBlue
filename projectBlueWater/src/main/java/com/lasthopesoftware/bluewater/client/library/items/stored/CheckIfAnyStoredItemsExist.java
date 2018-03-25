package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.namehillsoftware.handoff.promises.Promise;

public interface CheckIfAnyStoredItemsExist {
	Promise<Boolean> promiseIsAnyStoredItemsWithFiles();
}
