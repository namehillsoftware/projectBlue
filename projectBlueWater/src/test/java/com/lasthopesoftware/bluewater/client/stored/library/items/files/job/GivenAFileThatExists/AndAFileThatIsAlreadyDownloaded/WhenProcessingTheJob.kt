package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatExists.AndAFileThatIsAlreadyDownloaded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class WhenProcessingTheJob {

	private static List<StoredFileJobState> storedFileJobStatus;

	@BeforeClass
	public static void before() {
		final StoredFile storedFile = new StoredFile(new LibraryId(10), 1, new ServiceFile(1), "test-path", true);
		storedFile.setIsDownloadComplete(true);

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> {
				final File mockFile = mock(File.class);
				when(mockFile.exists()).thenReturn(true);
				return mockFile;
			},
			mock(AccessStoredFiles.class),
			(libraryId, f) -> Promise.empty(),
			f -> true,
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {});

		storedFileJobStatus = storedFileJobProcessor.observeStoredFileDownload(
			Collections.singleton(new StoredFileJob(new LibraryId(10), new ServiceFile(1), storedFile)))
			.map(f -> f.storedFileJobState)
			.toList().blockingGet();
	}

	@Test
	public void thenAnAlreadyExistsResultIsReturned() {
		assertThat(storedFileJobStatus).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloaded);
	}
}
