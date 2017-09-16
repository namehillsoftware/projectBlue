package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAFileThatExists.AndAnImpossibleFileRead;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static StoredFileReadException storedFileReadException;

	@BeforeClass
	public static void before() throws StoredFileJobException, StorageCreatePathException, StoredFileWriteException {
		final StoredFileJob storedFileJob = new StoredFileJob(
			storedFile -> {
				final File mockFile = mock(File.class);
				when(mockFile.exists()).thenReturn(true);
				return mockFile;
			},
			mock(IConnectionProvider.class),
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			mock(IFileReadPossibleArbitrator.class),
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {},
			new ServiceFile(1),
			new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true));

		try {
			storedFileJob.processJob();
		} catch (StoredFileReadException e) {
			storedFileReadException = e;
		}
	}

	@Test
	public void thenAStoredReadFileExceptionIsThrown() {
		assertThat(storedFileReadException).isNotNull();
	}
}
