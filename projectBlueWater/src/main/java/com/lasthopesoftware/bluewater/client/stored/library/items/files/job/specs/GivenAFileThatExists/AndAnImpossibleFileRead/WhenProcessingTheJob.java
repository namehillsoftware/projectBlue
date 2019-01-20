package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatExists.AndAnImpossibleFileRead;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static StoredFileReadException storedFileReadException;

	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			storedFile -> {
				final File mockFile = mock(File.class);
				when(mockFile.exists()).thenReturn(true);
				return mockFile;
			},
			mock(IStoredFileAccess.class),
			f -> Promise.empty(),
			mock(IFileReadPossibleArbitrator.class),
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {});

		try {
			storedFileJobProcessor.observeStoredFileDownload(
				Collections.singleton(new StoredFileJob(
					new ServiceFile(1),
					new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true))))
				.blockingSubscribe();
		} catch (Throwable e) {
			if (e.getCause() instanceof StoredFileReadException)
				storedFileReadException = (StoredFileReadException)e.getCause();
		}
	}

	@Test
	public void thenAStoredReadFileExceptionIsThrown() {
		assertThat(storedFileReadException).isNotNull();
	}
}
