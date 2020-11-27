package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.InAnotherLocation;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilesPropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenUpdatingTheFile extends AndroidContext {

	private static StoredFile storedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final MediaFileUriProvider mediaFileUriProvider = mock(MediaFileUriProvider.class);
		when(mediaFileUriProvider.promiseFileUri(any()))
			.thenReturn(Promise.empty());

		final MediaFileIdProvider mediaFileIdProvider = mock(MediaFileIdProvider.class);
		when(mediaFileIdProvider.getMediaId(any(), any()))
			.thenReturn(Promise.empty());

		final FakeFilesPropertiesProvider filePropertiesProvider = new FakeFilesPropertiesProvider();
		filePropertiesProvider.addFilePropertiesToCache(
			new ServiceFile(4),
			new HashMap<String, String>() {{
				put(KnownFileProperties.ARTIST, "artist");
				put(KnownFileProperties.ALBUM, "album");
				put(KnownFileProperties.FILENAME, "my-filename.mp3");
			}});

		final FakeLibraryProvider fakeLibraryProvider = new FakeLibraryProvider(new Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL));

		new FuturePromise<>(new StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
			mediaFileIdProvider,
			new StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryProvider,
			filePropertiesProvider,
			new SyncDirectoryLookup(
				fakeLibraryProvider,
				() -> new Promise<>(Collections.singletonList(new File("/my-public-drive-1"))),
				() -> new Promise<>(Collections.emptyList()),
				f -> 0)).promiseStoredFileUpdate(
					new LibraryId(14),
			new ServiceFile(4))).get();


		final StoredFileUpdater storedFileUpdater = new StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
			mediaFileIdProvider,
			new StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryProvider,
			filePropertiesProvider,
			new SyncDirectoryLookup(
				fakeLibraryProvider,
				() -> new Promise<>(Collections.singletonList(new File("/my-public-drive"))),
				() -> new Promise<>(Collections.emptyList()),
				f -> 0));

		storedFile = new FuturePromise<>(storedFileUpdater.promiseStoredFileUpdate(
			new LibraryId(14),
			new ServiceFile(4))).get();
	}

	@Test
	public void thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile.isOwner()).isTrue();
	}

	@Test
	public void thenTheFilePathIsCorrect() {
		assertThat(storedFile.getPath()).isEqualTo("/my-public-drive-1/14/artist/album/my-filename.mp3");
	}
}
