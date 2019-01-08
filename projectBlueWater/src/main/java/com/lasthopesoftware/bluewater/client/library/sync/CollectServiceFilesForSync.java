package com.lasthopesoftware.bluewater.client.library.sync;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface CollectServiceFilesForSync {
	Promise<Collection<ServiceFile>> promiseServiceFilesToSync();
}
