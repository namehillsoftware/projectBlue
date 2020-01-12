package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.specs.GivenATypicalLibrary.WithoutTheStoredFile.AndTheStoredFileExists;

import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.specs.FakeFilesPropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.specs.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenUpdatingTheFile extends AndroidContext {

	private static StoredFile storedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final MediaFileUriProvider mediaFileUriProvider = mock(MediaFileUriProvider.class);
		when(mediaFileUriProvider.promiseFileUri(new ServiceFile(4)))
			.thenReturn(new Promise<>(Uri.fromFile(new File("/custom-root/a-file.mp3"))));

		final MediaFileIdProvider mediaFileIdProvider = mock(MediaFileIdProvider.class);
		when(mediaFileIdProvider.getMediaId(new LibraryId(14), new ServiceFile(4)))
			.thenReturn(new Promise<>(12));

		final FakeFilesPropertiesProvider filePropertiesProvider = new FakeFilesPropertiesProvider();
		filePropertiesProvider.addFilePropertiesToCache(
			new ServiceFile(4),
			new HashMap<String, String>() {{
				put(KnownFileProperties.ARTIST, "artist");
				put(KnownFileProperties.ALBUM, "album");
				put(KnownFileProperties.FILENAME, "my-filename.mp3");
			}});

		final FakeLibraryProvider fakeLibraryProvider = new FakeLibraryProvider(Collections.singleton(new Library()
			.setIsUsingExistingFiles(true)
			.setId(14)
			.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)));

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
				() -> new Promise<>(Collections.emptyList())));

		storedFile = new FuturePromise<>(storedFileUpdater.promiseStoredFileUpdate(
			new LibraryId(14),
			new ServiceFile(4))).get();
	}

	@Test
	public void thenTheFileIsInsertedIntoTheDatabase() throws ExecutionException, InterruptedException {
		assertThat(new FuturePromise<>(
			new StoredFileQuery(ApplicationProvider.getApplicationContext()).promiseStoredFile(
				new LibraryId(14), new ServiceFile(4))).get()).isNotNull();
	}

	@Test
	public void thenTheFileIsNotOwnedByTheLibrary() {
		assertThat(storedFile.isOwner()).isFalse();
	}

	@Test
	public void thenTheFilePathIsCorrect() {
		assertThat(storedFile.getPath()).isEqualTo("/custom-root/a-file.mp3");
	}
}
