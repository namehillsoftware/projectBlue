package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.persistence;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.repository.CachedFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IDiskFileAccessTimeUpdater {
	Promise<CachedFile> promiseFileAccessedUpdate(CachedFile cachedFile);
}
