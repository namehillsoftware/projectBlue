package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.namehillsoftware.handoff.promises.Promise;

public class SyncChecker implements CheckForSync {
	@Override
	public Promise<Boolean> promiseIsSyncNeeded() {
		return new Promise<>(false);
	}
}
