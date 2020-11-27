package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenTheUriIsNotAvailable;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenProvidingTheUri {

	private static Uri cachedFileUri;

	@BeforeClass
	public static void before() {
		final RemoteFileUriProvider remoteFileUriProvider = mock(RemoteFileUriProvider.class);
		when(remoteFileUriProvider.promiseFileUri(new ServiceFile(10)))
			.thenReturn(Promise.empty());

		final ICachedFilesProvider cachedFilesProvider = mock(ICachedFilesProvider.class);
		when(cachedFilesProvider.promiseCachedFile("file?key=1"))
			.thenReturn(new Promise<>(new CachedFile()));

		final CachedAudioFileUriProvider cachedAudioFileUriProvider =
			new CachedAudioFileUriProvider(
				remoteFileUriProvider,
				cachedFilesProvider);

		cachedAudioFileUriProvider
			.promiseFileUri(new ServiceFile(10))
			.then(uri -> cachedFileUri = uri);
	}

	@Test
	public void thenTheUriIsEmpty() {
		assertThat(cachedFileUri).isNull();
	}
}
