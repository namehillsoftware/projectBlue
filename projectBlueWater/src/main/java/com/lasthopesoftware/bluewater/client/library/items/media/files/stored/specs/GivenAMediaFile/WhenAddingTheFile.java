package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.specs.GivenAMediaFile;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.sync.LookupSyncDirectory;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenAddingTheFile extends AndroidContext {

	private static StoredFile storedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final StoredFileAccess storedFileAccess = new StoredFileAccess(
			RuntimeEnvironment.application,
			new Library(),
			mock(LookupSyncDirectory.class),
			mock(GetAllStoredFilesInLibrary.class),
			mock(CachedFilePropertiesProvider.class));

		new FuturePromise<>(storedFileAccess.addMediaFile(
			new ServiceFile(3),
			14,
			"a-test-path")).get();

		storedFile = new FuturePromise<>(storedFileAccess.getStoredFile(new ServiceFile(3))).get();
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
