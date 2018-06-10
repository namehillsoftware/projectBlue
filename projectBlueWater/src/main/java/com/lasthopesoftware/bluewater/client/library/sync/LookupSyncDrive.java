package com.lasthopesoftware.bluewater.client.library.sync;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public interface LookupSyncDrive {
	Promise<File> promiseSyncDrive(Library library);
}
