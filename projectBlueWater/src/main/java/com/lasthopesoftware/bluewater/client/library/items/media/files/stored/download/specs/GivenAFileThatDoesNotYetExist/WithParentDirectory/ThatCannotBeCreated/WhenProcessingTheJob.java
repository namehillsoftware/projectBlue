package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAFileThatDoesNotYetExist.WithParentDirectory.ThatCannotBeCreated;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static StorageCreatePathException storageCreatePathException;

	@BeforeClass
	public static void before() throws StoredFileJobException, IOException {
		final IConnectionProvider fakeConnectionProvider = mock(IConnectionProvider.class);

		final StoredFileJob storedFileJob = new StoredFileJob(
			$ -> {
				final File file = mock(File.class);
				final File parentFile = mock(File.class);
				when(parentFile.mkdirs()).thenReturn(false);
				when(file.getParentFile()).thenReturn(parentFile);

				return file;
			},
			fakeConnectionProvider,
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> false,
			f -> true,
			(is, f) -> {},
			new ServiceFile(1),
			storedFile);

		try {
			storedFileJob.processJob();
		} catch (StorageCreatePathException e) {
			storageCreatePathException = e;
		}
	}

	@Test
	public void thenAStorageCreatePathExceptionIsThrown() {
		assertThat(storageCreatePathException).isNotNull();
	}
}
