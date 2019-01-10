package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.AndFileWritingIsNotPossible;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenProcessingTheJob {

	private static StoredFileWriteException storedFileWriteException;

	@BeforeClass
	public static void before() {
		final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
		storedFile.setIsDownloadComplete(true);

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			mock(IConnectionProvider.class),
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> false,
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {});

		try {
			storedFileJobProcessor.observeStoredFileDownload(
				new StoredFileJob(new ServiceFile(1), storedFile)).blockingSubscribe();
		} catch (Throwable e) {
			if (e.getCause() instanceof StoredFileWriteException)
				storedFileWriteException = (StoredFileWriteException)e.getCause();
		}
	}

	@Test
	public void thenAStoredFileWriteExceptionIsThrown() {
		assertThat(storedFileWriteException).isNotNull();
	}
}
