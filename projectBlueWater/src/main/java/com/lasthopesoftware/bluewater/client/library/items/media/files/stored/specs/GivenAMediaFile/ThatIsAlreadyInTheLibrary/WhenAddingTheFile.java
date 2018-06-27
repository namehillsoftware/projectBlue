package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.specs.GivenAMediaFile.ThatIsAlreadyInTheLibrary;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs.FakeCachedFilesPropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.sync.LookupSyncDirectory;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenAddingTheFile extends AndroidContext {

	private static StoredFile storedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final FakeCachedFilesPropertiesProvider cachedFilesPropertiesProvider = new FakeCachedFilesPropertiesProvider();
		cachedFilesPropertiesProvider.addFilePropertiesToCache(
			new ServiceFile(3),
			new HashMap<String, String>() {{
				put(FilePropertiesProvider.ARTIST, "a");
				put(FilePropertiesProvider.ALBUM, "a");
				put(FilePropertiesProvider.FILENAME, "a-test-path");
			}});

		final LookupSyncDirectory mockSyncDrive = mock(LookupSyncDirectory.class);
		when(mockSyncDrive.promiseSyncDrive(any()))
			.thenReturn(new Promise<>(new File("/a-path")));

		final StoredFileAccess storedFileAccess = new StoredFileAccess(
			RuntimeEnvironment.application,
			mockSyncDrive,
			mock(GetAllStoredFilesInLibrary.class),
			cachedFilesPropertiesProvider);

		final Library library = new Library().setId(15);

		new FuturePromise<>(
			storedFileAccess
				.promiseStoredFileUpsert(library, new ServiceFile(3))
				.eventually(storedFileAccess::markStoredFileAsDownloaded)).get();

		new FuturePromise<>(storedFileAccess.addMediaFile(
			library,
			new ServiceFile(3),
			14,
			"/a-path/a/a/a-test-path")).get();

		storedFile = new FuturePromise<>(storedFileAccess.getStoredFile(library, new ServiceFile(3))).get();
	}

	@Test
	public void thenTheLibraryIdIsCorrect() {
		assertThat(storedFile.getLibraryId()).isEqualTo(15);
	}

	@Test
	public void thenThisLibraryDoesOwnTheFile() {
		assertThat(storedFile.isOwner()).isTrue();
	}

	@Test
	public void thenTheDownloadIsMarkedComplete() {
		assertThat(storedFile.isDownloadComplete()).isTrue();
	}

	@Test
	public void thenTheStoredFileHasTheCorrectMediaFileId() {
		assertThat(storedFile.getStoredMediaId()).isEqualTo(14);
	}

	@Test
	public void thenTheStoredFileHasTheCorrectPath() {
		assertThat(storedFile.getPath()).isEqualTo("/a-path/a/a/a-test-path");
	}
}
