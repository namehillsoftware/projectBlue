package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public interface ICache {
	Promise<CachedFile> put(final String uniqueKey, final byte[] fileData);

	Promise<File> promiseCachedFile(final String uniqueKey);
}
