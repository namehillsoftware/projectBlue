package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import okio.Buffer;


class DiskFileCacheDataSource implements DataSource {

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCacheDataSource.class);

	private final HttpDataSource defaultHttpDataSource;
	private final String serviceFileKey;
	private final DiskFileCache diskFileCache;
	private Buffer buffer;

	DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ServiceFile serviceFile, DiskFileCache diskFileCache) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		serviceFileKey = String.valueOf(serviceFile.getKey());
		this.diskFileCache = diskFileCache;
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		if (dataSpec.position == 0)
			 buffer = new Buffer();

		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] bytes, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(bytes, offset, readLength);

		if (buffer == null) return result;

		if (result != C.RESULT_END_OF_INPUT) {
			buffer.write(bytes, offset, result);
			return result;
		}

		diskFileCache
			.put(serviceFileKey, buffer)
			.excuse(e -> {
				logger.warn("An error occurred storing the audio file", e);
				return null;
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

		if (buffer != null)
			buffer.close();
	}
}
