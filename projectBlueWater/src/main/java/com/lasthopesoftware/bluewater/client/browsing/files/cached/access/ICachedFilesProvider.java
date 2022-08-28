package com.lasthopesoftware.bluewater.client.browsing.files.cached.access;

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface ICachedFilesProvider {
	Promise<CachedFile> promiseCachedFile(String uniqueKey);
}
