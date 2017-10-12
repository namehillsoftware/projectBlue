package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;
import java.util.Arrays;


class DiskFileCacheDataSource implements DataSource {

	private final HttpDataSource defaultHttpDataSource;
	private final String serviceFileKey;
	private final DiskFileCache diskFileCache;
	private final Object promiseFileSync = new Object();
	private Promise<Void> promiseFileAppend = Promise.empty();

	DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ServiceFile serviceFile, DiskFileCache diskFileCache) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		serviceFileKey = String.valueOf(serviceFile.getKey());
		this.diskFileCache = diskFileCache;
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] buffer, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(buffer, offset, readLength);

		final byte[] copiedBuffer = Arrays.copyOf(buffer, buffer.length);

		synchronized (promiseFileSync) {
			promiseFileAppend =
				promiseFileAppend
					.eventually(v -> diskFileCache.putOrAppend(serviceFileKey, copiedBuffer));
		}

		return result;
	}

	@Override
	public Uri getUri() {
		return defaultHttpDataSource.getUri();
	}

	@Override
	public void close() throws IOException {
		defaultHttpDataSource.close();
	}
}
