package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.AndFileWritingIsNotPossible;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenProcessingTheJob {

	private static StoredFileWriteException storedFileWriteException;

	@BeforeClass
	public static void before() {
		final StoredFile storedFile = new StoredFile(new LibraryId(1), 1, new ServiceFile(1), "test-path", true);
		storedFile.setIsDownloadComplete(true);

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			mock(IStoredFileAccess.class),
			(libraryId, f) -> Promise.empty(),
			f -> false,
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {});

		try {
			storedFileJobProcessor.observeStoredFileDownload(Collections.singleton(
				new StoredFileJob(new LibraryId(1), new ServiceFile(1), storedFile))).blockingSubscribe();
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
