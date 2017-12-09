package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.CachedFileOutputStream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.supplier.ICacheStreamSupplier;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import okio.Buffer;


class DiskFileCacheDataSource implements DataSource {

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCacheDataSource.class);
	private static final long maxBufferSize = 5 * 1024 * 1024; // 5MB

	private final HttpDataSource defaultHttpDataSource;
	private final String serviceFileKey;
	private final ICacheStreamSupplier cacheStreamSupplier;
	private Buffer buffer;
	private Promise<CachedFileOutputStream> promisedOutputStream;

	DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ServiceFile serviceFile, ICacheStreamSupplier cacheStreamSupplier) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		serviceFileKey = String.valueOf(serviceFile.getKey());
		this.cacheStreamSupplier = cacheStreamSupplier;
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		if (dataSpec.position == 0) {
			buffer = new Buffer();
			promisedOutputStream = cacheStreamSupplier.promiseCachedFileOutputStream(serviceFileKey);
		}

		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] bytes, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(bytes, offset, readLength);

		if (buffer == null) return result;

		if (result == C.RESULT_END_OF_INPUT) {
			Promise<CachedFileOutputStream> outputStream = promisedOutputStream;

			if (buffer.size() > 0) {
				outputStream = promisedOutputStream
					.eventually(cachedFileOutputStream -> {
						final Promise<CachedFileOutputStream> promisedWrite =
							cachedFileOutputStream.promiseWrite(buffer);

						promisedWrite.then(
							os -> {
								buffer.close();
								return null;
							},
							e -> {
								logger.warn("An error occurred storing the audio file", e);
								buffer.close();
								cachedFileOutputStream.close();
								return null;
							});

						return promisedWrite;
					});
			}

			outputStream.eventually(cachedFileOutputStream ->
				cachedFileOutputStream
					.flush()
					.eventually(
						os -> {
							os.close();
							buffer.close();
							return os.commitToCache();
						},
						e -> {
							logger.warn("An error occurred flushing the output stream", e);
							cachedFileOutputStream.close();
							buffer.close();
							return Promise.empty();
						}));

			return result;
		}

		buffer.write(bytes, offset, result);

		if (buffer.size() <= maxBufferSize) return result;

		final Buffer bufferToWrite = buffer;
		buffer = new Buffer();

		promisedOutputStream = promisedOutputStream
			.eventually(cachedFileOutputStream -> {
				final Promise<CachedFileOutputStream> promisedWrite =
					cachedFileOutputStream.promiseWrite(bufferToWrite);

				promisedWrite.then(
					os -> {
						bufferToWrite.close();
						return null;
					},
					e -> {
						logger.warn("An error occurred storing the audio file", e);
						bufferToWrite.close();
						cachedFileOutputStream.close();
						return null;
					});

				return promisedWrite;
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
