package com.lasthopesoftware.bluewater.client.browsing.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.CacheOutputStream;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.supplier.ICacheStreamSupplier;
import com.lasthopesoftware.resources.uri.PathAndQuery;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okio.Buffer;


public class DiskFileCacheDataSource implements DataSource {

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCacheDataSource.class);
	private static final long maxBufferSize = 1024 * 1024; // 1MB

	private final HttpDataSource defaultHttpDataSource;
	private final ICacheStreamSupplier cacheStreamSupplier;
	private Buffer buffer;
	private Promise<CacheOutputStream> promisedOutputStream;

	public DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ICacheStreamSupplier cacheStreamSupplier) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		this.cacheStreamSupplier = cacheStreamSupplier;
	}

	@Override
	public void addTransferListener(TransferListener transferListener) {

	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		if (dataSpec.position == 0) {
			buffer = new Buffer();
			promisedOutputStream = cacheStreamSupplier.promiseCachedFileOutputStream(PathAndQuery.forUri(dataSpec.uri));
		}

		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] bytes, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(bytes, offset, readLength);

		if (buffer == null) return result;

		if (result != C.RESULT_END_OF_INPUT) {

			buffer.write(bytes, offset, result);

			if (buffer.size() <= maxBufferSize) return result;

			final Buffer bufferToWrite = buffer;
			buffer = new Buffer();

			promisedOutputStream = promisedOutputStream
				.eventually(cachedFileOutputStream -> {
					final Promise<CacheOutputStream> promisedWrite =
						cachedFileOutputStream.promiseTransfer(bufferToWrite);

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

		final Promise<CacheOutputStream> outputStream = buffer.size() == 0
			? promisedOutputStream
			: promisedOutputStream
				.eventually(cachedFileOutputStream -> {
					final Promise<CacheOutputStream> promisedWrite =
						cachedFileOutputStream.promiseTransfer(buffer);

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

	@Override
	public Uri getUri() {
		return defaultHttpDataSource.getUri();
	}

	@Override
	public Map<String, List<String>> getResponseHeaders() {
		return null;
	}

	@Override
	public void close() throws IOException {
		defaultHttpDataSource.close();
	}
}
