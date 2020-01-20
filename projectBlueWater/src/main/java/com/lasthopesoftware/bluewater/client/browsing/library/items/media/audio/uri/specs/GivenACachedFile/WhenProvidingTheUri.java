package com.lasthopesoftware.bluewater.client.browsing.library.items.media.audio.uri.specs.GivenACachedFile;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.audio.uri.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.uri.RemoteFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenProvidingTheUri {

	private static Uri cachedFileUri;

	private static final File file;

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
	}

	@BeforeClass
	public static void before() {
		final Uri remoteUri = Uri.parse("http://a-url/file?key=1");
		final RemoteFileUriProvider remoteFileUriProvider = mock(RemoteFileUriProvider.class);
		when(remoteFileUriProvider.promiseFileUri(new ServiceFile(10)))
			.thenReturn(new Promise<>(remoteUri));

		final ICachedFilesProvider cachedFilesProvider = mock(ICachedFilesProvider.class);
		when(cachedFilesProvider.promiseCachedFile(remoteUri.getPath() + "?" + remoteUri.getQuery()))
			.thenReturn(new Promise<>(new CachedFile().setFileName(file.getAbsolutePath())));

		final CachedAudioFileUriProvider cachedAudioFileUriProvider =
			new CachedAudioFileUriProvider(
				remoteFileUriProvider,
				cachedFilesProvider);

		cachedAudioFileUriProvider
			.promiseFileUri(new ServiceFile(10))
			.then(uri -> cachedFileUri = uri);
	}

	@Test
	public void thenTheUriIsThePathToTheFile() {
		assertThat(cachedFileUri.toString()).isEqualTo(Uri.fromFile(file).toString());
	}
}
