package com.lasthopesoftware.bluewater.client.library.items.media.audio.specs.GivenAFileLessThan5Megabytes;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.DiskFileCacheDataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.CacheOutputStream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.supplier.ICacheStreamSupplier;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Random;

import okio.Buffer;
import okio.BufferedSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenStreamingTheFileInOddChunks {

	private static final byte[] bytesWritten = new byte[2 * 1024 * 1024];
	private static final byte[] bytes = new byte[2 * 1024 * 1024];

	static {
		new Random().nextBytes(bytes);
	}

	@BeforeClass
	public static void context() throws IOException {
		final ICacheStreamSupplier fakeCacheStreamSupplier = uniqueKey -> new Promise<>(new CacheOutputStream() {
				int numberOfBytesWritten = 0;

				@Override
				public Promise<CacheOutputStream> promiseWrite(byte[] buffer, int offset, int length) {
					return new Promise<>(this);
				}

				@Override
				public Promise<CacheOutputStream> promiseTransfer(BufferedSource bufferedSource) {
					try {
						while (numberOfBytesWritten < bytesWritten.length) {
							int read = bufferedSource.read(bytesWritten, numberOfBytesWritten, bytesWritten.length - numberOfBytesWritten);
							if (read == -1) return new Promise<>(this);
							numberOfBytesWritten += read;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					return new Promise<>(this);
				}

				@Override
				public Promise<CachedFile> commitToCache() {
					return new Promise<>(new CachedFile());
				}

				@Override
				public Promise<CacheOutputStream> flush() {
					return new Promise<>(this);
				}

				@Override
				public void close() throws IOException {

				}
			});

		final Buffer buffer = new Buffer();
		buffer.write(bytes);

		final HttpDataSource dataSource = mock(HttpDataSource.class);
		when(dataSource.read(any(), anyInt(), anyInt()))
			.then(invocation -> {
				if (buffer.exhausted()) return C.RESULT_END_OF_INPUT;

				final byte[] bytes = invocation.getArgument(0);
				final int offset = invocation.getArgument(1);
				final int length = invocation.getArgument(2);
				return buffer.read(bytes, offset, length);
			});

		final DiskFileCacheDataSource diskFileCacheDataSource =
			new DiskFileCacheDataSource(
				dataSource,
				fakeCacheStreamSupplier);

		diskFileCacheDataSource.open(new DataSpec(Uri.parse("http://my-server/file"), 0, 2 * 1024 * 1024, "1"));

		final Random random = new Random();
		int readResult;
		do {
			final byte[] bytes = new byte[random.nextInt(1000000)];
			readResult = diskFileCacheDataSource.read(bytes, 0, bytes.length);
		} while (readResult != C.RESULT_END_OF_INPUT);
	}

	@Test
	public void thenTheEntireFileIsWritten() {
		Assert.assertArrayEquals(bytes, bytesWritten);
	}
}
