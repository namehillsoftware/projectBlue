package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class DiskFileCacheDataSource implements DataSource {

	private static final ExecutorService memoryCacheExecutorService = Executors.newSingleThreadExecutor();

	private final HttpDataSource defaultHttpDataSource;
	private final ServiceFile serviceFile;
	private final DiskFileCache diskFileCache;
	private final ArrayList<Byte> cachedData = new ArrayList<>();

	DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ServiceFile serviceFile, DiskFileCache diskFileCache) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		this.serviceFile = serviceFile;
		this.diskFileCache = diskFileCache;
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] buffer, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(buffer, offset, readLength);
		memoryCacheExecutorService.execute(() -> {
			for (final byte b : buffer)
				cachedData.add(b);
		});

		if (result != C.RESULT_END_OF_INPUT) return result;

		memoryCacheExecutorService.execute(() -> {
			final byte[] cachedDataArray = new byte[cachedData.size()];
			for (int i = 0; i < cachedData.size(); i++)
				cachedDataArray[i] = cachedData.get(i);

			diskFileCache.put(String.valueOf(serviceFile.getKey()), cachedDataArray);
		});

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
