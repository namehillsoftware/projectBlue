package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.CachedFileOutputStream;
import com.lasthopesoftware.messenger.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;


class DiskFileCacheDataSource implements DataSource {

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCacheDataSource.class);

	private final HttpDataSource defaultHttpDataSource;
	private final String serviceFileKey;
	private final DiskFileCache diskFileCache;
	private final Object promiseFileSync = new Object();
	private Promise<CachedFileOutputStream> promisedOutputStream;
	private boolean isClosed;

	DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ServiceFile serviceFile, DiskFileCache diskFileCache) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		serviceFileKey = String.valueOf(serviceFile.getKey());
		this.diskFileCache = diskFileCache;
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		promisedOutputStream = diskFileCache.promiseCachedFileOutputStream(serviceFileKey);
		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] buffer, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(buffer, offset, readLength);

		if (result == C.RESULT_END_OF_INPUT) {
			promisedOutputStream
				.eventually(CachedFileOutputStream::flush)
				.eventually(cachedFileOutputStream -> {
					cachedFileOutputStream.close();
					return cachedFileOutputStream.commitToCache();
				});

			return result;
		}

		final byte[] copiedBuffer = Arrays.copyOfRange(buffer, offset, offset + result);

		synchronized (promiseFileSync) {
			promisedOutputStream = promisedOutputStream
				.eventually(cachedFileOutputStream -> {
					if (isClosed) return new Promise<>(cachedFileOutputStream);

					final Promise<CachedFileOutputStream> promisedWrite = cachedFileOutputStream.promiseWrite(copiedBuffer, 0, copiedBuffer.length);
					promisedWrite.excuse(e -> {
						logger.warn("An error occurred storing the audio file", e);
						cachedFileOutputStream.close();
						return null;
					});

					return promisedWrite;
				});
		}

		return result;
	}

	@Override
	public Uri getUri() {
		return defaultHttpDataSource.getUri();
	}

	@Override
	public void close() throws IOException {
		isClosed = true;
		defaultHttpDataSource.close();

		if (promisedOutputStream == null) return;

		promisedOutputStream.then(cachedFileOutputStream -> {
			cachedFileOutputStream.close();
			return null;
		});
	}
}
