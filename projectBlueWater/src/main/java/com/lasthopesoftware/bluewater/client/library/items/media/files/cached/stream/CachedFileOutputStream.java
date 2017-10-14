package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.IDiskFileCachePersistence;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.File;

public class CachedFileOutputStream {

	public CachedFileOutputStream(String uniqueKey, File file, IDiskFileCachePersistence diskFileCachePersistence) {

	}

	public Promise<Long> write(byte[] buffer, int offset, int length) {
		return Promise.empty();
	}

	public Promise<Void> commitToCache() {
		return Promise.empty();
	}
}
