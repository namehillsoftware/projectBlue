package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

public class SyncChecker implements CheckForSync {

	private final CollectServiceFilesForSync serviceFilesForSync;

	public SyncChecker(CollectServiceFilesForSync serviceFilesForSync) {
		this.serviceFilesForSync = serviceFilesForSync;
	}

	@Override
	public Promise<Boolean> promiseIsSyncNeeded() {
		return serviceFilesForSync.promiseServiceFilesToSync(new LibraryId(8)).then(sf -> sf.size() > 0);
	}
}
