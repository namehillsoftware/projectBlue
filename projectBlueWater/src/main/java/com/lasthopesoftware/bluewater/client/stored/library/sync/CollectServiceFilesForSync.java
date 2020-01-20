package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface CollectServiceFilesForSync {
	Promise<Collection<ServiceFile>> promiseServiceFilesToSync(LibraryId libraryId);
}
