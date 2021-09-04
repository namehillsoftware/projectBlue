package com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.GivenAFileThatIsAvailableRemotely.AndAvailableOnDisk.AndExistingFileUsageIsNotAllowed;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingTheUri {

	private static Throwable rejection;
	private static Uri returnedFileUri;

	@BeforeClass
	public static void context() throws Throwable {

		final StoredFileUriProvider mockStoredFileUriProvider = mock(StoredFileUriProvider.class);
		when(mockStoredFileUriProvider.promiseFileUri(any()))
			.thenReturn(Promise.empty());

		final CachedAudioFileUriProvider cachedAudioFileUriProvider = mock(CachedAudioFileUriProvider.class);
		when(cachedAudioFileUriProvider.promiseFileUri(new ServiceFile(3)))
			.thenReturn(Promise.empty());

		final MediaFileUriProvider mockMediaFileUriProvider = mock(MediaFileUriProvider.class);
		when(mockMediaFileUriProvider.promiseFileUri(any()))
			.thenReturn(new Promise<>(Uri.fromFile(new File("/a_media_path/to_a_file.mp3"))));

		final RemoteFileUriProvider mockRemoteFileUriProvider = mock(RemoteFileUriProvider.class);
		when(mockRemoteFileUriProvider.promiseFileUri(new ServiceFile(3)))
			.thenReturn(new Promise<>(Uri.parse("http://remote-url/to_a_file.mp3")));

		final BestMatchUriProvider bestMatchUriProvider =
			new BestMatchUriProvider(
				new Library(),
				mockStoredFileUriProvider,
				cachedAudioFileUriProvider,
				mockMediaFileUriProvider,
				mockRemoteFileUriProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		bestMatchUriProvider
			.promiseFileUri(new ServiceFile(3))
			.then(f -> {
				returnedFileUri = f;
				countDownLatch.countDown();
				return null;
			})
			.excuse(e -> {
				rejection = e;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();

		if (rejection != null)
			throw rejection;
	}

	@Test
	public void thenTheRemoteFileUriIsReturned() {
		assertThat(returnedFileUri.toString()).isEqualTo("http://remote-url/to_a_file.mp3");
	}
}
