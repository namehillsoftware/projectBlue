package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAFileThatExists.AndAFileThatIsAlreadyDownloaded;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResultOptions;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static StoredFileJobResult storedFileJobResult;

	@BeforeClass
	public static void before() throws StoredFileJobException, StorageCreatePathException, StoredFileWriteException, StoredFileReadException {
		final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
		storedFile.setIsDownloadComplete(true);

		final StoredFileJob storedFileJob = new StoredFileJob(
			$ -> {
				final File mockFile = mock(File.class);
				when(mockFile.exists()).thenReturn(true);
				return mockFile;
			},
			mock(IConnectionProvider.class),
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> true,
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {},
			new ServiceFile(1),
			storedFile);

		storedFileJobResult = storedFileJob.processJob();
	}

	@Test
	public void thenAnAlreadyExistsResultIsReturned() {
		assertThat(storedFileJobResult.storedFileJobResult).isEqualTo(StoredFileJobResultOptions.AlreadyExists);
	}
}
