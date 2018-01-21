package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.specs.GivenATypicalFile.AndABufferedSource;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.IDiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.CachedFileOutputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenWritingToAFileMultipleTimes {

	private static final File file;
	private static final byte[] bytes = new byte[2000000];

	private static final byte[] bytesWritten = new byte[2000000];

	private static final Buffer firstBuffer = new Buffer();
	private static final Buffer secondBuffer = new Buffer();

	static {
		File file1;
		try {
			file1 = File.createTempFile("temp", ".txt");
		} catch (IOException e) {
			e.printStackTrace();
			file1 = new File("test");
		}

		file = file1;
		file.deleteOnExit();
		new Random().nextBytes(bytes);

		firstBuffer.write(bytes, 0, 1000000);
		secondBuffer.write(bytes, 1000000, 1000000);
	}

	@BeforeClass
	public static void before() throws InterruptedException {
		final CachedFileOutputStream cachedFileOutputStream = new CachedFileOutputStream("unique-test", file, mock(IDiskFileCachePersistence.class));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		cachedFileOutputStream.promiseTransfer(firstBuffer)
			.eventually(os -> os.promiseTransfer(secondBuffer))
			.then(w -> {
				try (final FileInputStream fis = new FileInputStream(file)) {
					fis.read(bytesWritten, 0, bytesWritten.length);
				}
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheBytesAreWrittenCorrectly() {
		assertThat(bytesWritten).isEqualTo(bytes);
	}
}
