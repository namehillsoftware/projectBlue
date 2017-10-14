package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.messenger.promises.Promise;

public interface ICachedFilesProvider {
	Promise<CachedFile> promiseCachedFile(String uniqueKey);
}
