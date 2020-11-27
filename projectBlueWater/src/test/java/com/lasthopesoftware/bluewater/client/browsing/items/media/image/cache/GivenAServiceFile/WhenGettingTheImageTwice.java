package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.GivenAServiceFile;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.GetRawImages;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheImageTwice {

	private static final byte[] expectedImageBytes = new byte[] { 18 };

	private static byte[] imageBytes;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final GetRawImages images = mock(GetRawImages.class);
		when(images.promiseImageBytes(new LibraryId(33), new ServiceFile(5555)))
			.thenReturn(new Promise<>(expectedImageBytes));

		final MemoryCachedImageAccess memoryCachedImageAccess = new MemoryCachedImageAccess(
			images,
			(libraryId, serviceFile) -> new Promise<>("the-key"));

		new FuturePromise<>(memoryCachedImageAccess.promiseImageBytes(new LibraryId(33), new ServiceFile(5555))).get();

		when(images.promiseImageBytes(new LibraryId(33), new ServiceFile(5555)))
			.thenReturn(new Promise<>(new byte[] { 8 }));

		imageBytes = new FuturePromise<>(memoryCachedImageAccess.promiseImageBytes(
			new LibraryId(33), new ServiceFile(5555))).get();
	}

	@Test
	public void thenTheCachedBytesAreCorrect() {
		assertThat(imageBytes).isEqualTo(expectedImageBytes);
	}
}
