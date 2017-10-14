package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence;

import com.lasthopesoftware.messenger.promises.Promise;

import java.io.File;

public interface IDiskFileCachePersistence {
	Promise<Void> putIntoDatabase(final String uniqueKey, final File file);
}
