package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public interface IDiskFileCachePersistence {
	Promise<CachedFile> putIntoDatabase(final String uniqueKey, final File file);
}
