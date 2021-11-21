package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatExists.AndAnImpossibleFileRead;

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
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class WhenProcessingTheJob {

	private static List<StoredFileJobState> jobStates;

	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			storedFile -> {
				final File mockFile = mock(File.class);
				when(mockFile.exists()).thenReturn(true);
				return mockFile;
			},
			mock(AccessStoredFiles.class),
			(libraryId, f) -> Promise.empty(),
			mock(IFileReadPossibleArbitrator.class),
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {});

		jobStates = storedFileJobProcessor.observeStoredFileDownload(
			Collections.singleton(new StoredFileJob(
				new LibraryId(12),
				new ServiceFile(1),
				new StoredFile(new LibraryId(12), 1, new ServiceFile(1), "test-path", true))))
			.map(j -> j.storedFileJobState)
			.toList()
			.blockingGet();
	}

	@Test
	public void thenTheFileStateIsUnreadable() {
		assertThat(jobStates).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Unreadable);
	}
}
