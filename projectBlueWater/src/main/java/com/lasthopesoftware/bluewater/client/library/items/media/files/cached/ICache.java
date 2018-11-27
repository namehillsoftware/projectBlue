package com.lasthopesoftware.bluewater.client.library.items.media.files.cached;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.namehillsoftware.handoff.promises.Promise;
import okio.Buffer;

import java.io.File;

public interface ICache {
	Promise<CachedFile> put(final String uniqueKey, final byte[] fileData);
	Promise<CachedFile> put(final String uniqueKey, final Buffer buffer);
	Promise<File> promiseCachedFile(final String uniqueKey);
	Promise<Boolean> containsKey(final String uniqueKey);
}
