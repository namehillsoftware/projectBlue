package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.namehillsoftware.handoff.promises.Promise;
import okio.BufferedSource;

import java.io.Closeable;

public interface CacheOutputStream extends Closeable {
	Promise<CacheOutputStream> promiseWrite(byte[] buffer, int offset, int length);
	Promise<CacheOutputStream> promiseTransfer(BufferedSource bufferedSource);
	Promise<CachedFile> commitToCache();
	Promise<CacheOutputStream> flush();
}
