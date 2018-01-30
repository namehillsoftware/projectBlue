package com.lasthopesoftware.bluewater.client.library.items.media.files.uri.specs.GivenAFileThatIsStoredCachedAndAvailableRemotely;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheUri {

	private static Throwable rejection;
	private static Uri returnedFileUri;

	public static void context() throws Throwable {
		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.getStoredFile(new ServiceFile(3))).thenReturn(
			new Promise<>(new StoredFile(
				new Library(),
				1,
				new ServiceFile(3),
				"file:///a_path",
				true)));

		final BestMatchUriProvider bestMatchUriProvider =
			new BestMatchUriProvider(
				new Library(),
				new StoredFileUriProvider(
					storedFileAccess,
					() -> true),
				mock(MediaFileUriProvider.class),
				mock(RemoteFileUriProvider.class));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		bestMatchUriProvider
			.getFileUri(new ServiceFile(3))
			.then(f -> {
				returnedFileUri = f;
				countDownLatch.countDown();
				return null;
			})
			.excuse(e -> {
				rejection = e;
				return null;
			});

		countDownLatch.await();

		if (rejection != null)
			throw rejection;
	}

	@Test
	public void thenTheStoredFileUriIsReturned() {
		assertThat(returnedFileUri.toString()).isEqualTo("file:///a_path");
	}
}
