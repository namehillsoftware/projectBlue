package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.specs.GivenATypicalFile;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.CachedFileOutputStream;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenCommittingTheFileToCache {

	private static final File mockedFile = mock(File.class);
	private static File persistedFile;
	private static String persistedKey;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CachedFileOutputStream cachedFileOutputStream = new CachedFileOutputStream("unique-test", mockedFile, (uniqueKey, file) -> {
			persistedKey = uniqueKey;
			persistedFile = file;
			return Promise.empty();
		});

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		cachedFileOutputStream.commitToCache()
			.then(v -> {
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheCorrectKeyIsPersisted() {
		assertThat(persistedKey).isEqualTo("unique-test");
	}

	@Test
	public void thenTheCorrectFileIsPersisted() {
		assertThat(persistedFile).isEqualTo(mockedFile);
	}
}
