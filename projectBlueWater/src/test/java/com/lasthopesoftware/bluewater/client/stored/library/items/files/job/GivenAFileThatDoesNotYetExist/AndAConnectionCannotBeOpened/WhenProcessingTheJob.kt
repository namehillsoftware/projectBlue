package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndAConnectionCannotBeOpened;

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
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new LibraryId(4), 1, new ServiceFile(1), "test-path", true);
	private static List<StoredFileJobState> jobStates;

	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> {
				final File file = mock(File.class);
				final File parentFile = mock(File.class);
				when(parentFile.mkdirs()).thenReturn(true);
				when(file.getParentFile()).thenReturn(parentFile);

				return file;
			},
			mock(AccessStoredFiles.class),
			(libraryId, f) -> new Promise<>(new IOException()),
			f -> false,
			f -> true,
			(is, f) -> {});

		jobStates = storedFileJobProcessor.observeStoredFileDownload(
			Collections.singleton(new StoredFileJob(new LibraryId(4), new ServiceFile(1), storedFile)))
				.map(s -> s.storedFileJobState)
				.toList()
				.blockingGet();
	}

	@Test
	public void thenTheStoredFileJobStateIsQueuedAgain() {
		assertThat(jobStates).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Downloading,
			StoredFileJobState.Queued);
	}
}
