package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenAMediaFile;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenAddingTheFile extends AndroidContext {

	private static StoredFile storedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final StoredFileAccess storedFileAccess = new StoredFileAccess(
			ApplicationProvider.getApplicationContext(),
			mock(GetAllStoredFilesInLibrary.class));

		final Library library = new Library().setId(15);

		storedFile = new FuturePromise<>(
			storedFileAccess.addMediaFile(
				library,
				new ServiceFile(3),
				14,
				"a-test-path")
				.eventually(v -> storedFileAccess.getStoredFile(library, new ServiceFile(3)))).get();
	}

	@Test
	public void thenTheLibraryIdIsCorrect() {
		assertThat(storedFile.getLibraryId()).isEqualTo(15);
	}

	@Test
	public void thenThisLibraryDoesNotOwnTheFile() {
		assertThat(storedFile.isOwner()).isFalse();
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
		assertThat(storedFile.getPath()).isEqualTo("a-test-path");
	}
}
