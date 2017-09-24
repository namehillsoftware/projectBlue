package com.lasthopesoftware.bluewater.client.library.sync;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.Collection;

public interface IServiceFilesToSyncCollector {
	Promise<Collection<ServiceFile>> promiseServiceFilesToSync();
}
