package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;


class DiskFileCacheDataSource implements DataSource {

	private static final ExecutorService memoryCacheExecutorService = Executors.newSingleThreadExecutor();

	private final HttpDataSource defaultHttpDataSource;
	private final ServiceFile serviceFile;
	private final DiskFileCache diskFileCache;
	private final ByteSink byteSink = new ByteSink();

	DiskFileCacheDataSource(HttpDataSource defaultHttpDataSource, ServiceFile serviceFile, DiskFileCache diskFileCache) {
		this.defaultHttpDataSource = defaultHttpDataSource;
		this.serviceFile = serviceFile;
		this.diskFileCache = diskFileCache;
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		diskFileCache.putEventually(String.valueOf(serviceFile.getKey()), Observable.create(byteSink));
		return defaultHttpDataSource.open(dataSpec);
	}

	@Override
	public int read(byte[] buffer, int offset, int readLength) throws IOException {
		final int result = defaultHttpDataSource.read(buffer, offset, readLength);
		memoryCacheExecutorService.execute(() -> byteSink.publish(buffer));

		if (result != C.RESULT_END_OF_INPUT) return result;

		memoryCacheExecutorService.execute(byteSink::closeSink);

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

	private static class ByteSink implements ObservableOnSubscribe<byte[]> {

		private final Queue<byte[]> cachedData = new ConcurrentLinkedQueue<>();
		private ObservableEmitter<byte[]> emitter;

		void publish(byte[] chunk) {
			cachedData.offer(chunk);
			drainBytes();
		}

		@Override
		public void subscribe(@NonNull ObservableEmitter<byte[]> e) throws Exception {
			emitter = e;

			drainBytes();
		}

		private synchronized void drainBytes() {
			if (emitter == null) return;

			byte[] b;
			while ((b = cachedData.poll()) != null)
				emitter.onNext(b);
		}

		void closeSink() {
			if (emitter != null)
				emitter.onComplete();
		}
	}
}
