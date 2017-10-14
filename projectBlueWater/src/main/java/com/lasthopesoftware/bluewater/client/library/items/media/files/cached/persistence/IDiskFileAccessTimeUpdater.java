package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.messenger.promises.Promise;

public interface IDiskFileAccessTimeUpdater {
	Promise<CachedFile> promiseFileAccessedUpdate(CachedFile cachedFile);
}
