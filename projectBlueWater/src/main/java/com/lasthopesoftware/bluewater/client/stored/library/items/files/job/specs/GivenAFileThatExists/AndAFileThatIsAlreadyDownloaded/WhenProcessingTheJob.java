package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatExists.AndAFileThatIsAlreadyDownloaded;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static List<StoredFileJobState> storedFileJobStatus;

	@BeforeClass
	public static void before() {
		final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
		storedFile.setIsDownloadComplete(true);

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
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
			(is, f) -> {});

		storedFileJobStatus = storedFileJobProcessor.observeStoredFileDownload(
			new StoredFileJob(new ServiceFile(1), storedFile))
			.map(f -> f.storedFileJobState)
			.toList().blockingGet();
	}

	@Test
	public void thenAnAlreadyExistsResultIsReturned() {
		assertThat(storedFileJobStatus).containsExactly(StoredFileJobState.Queued, StoredFileJobState.AlreadyExists);
	}
}
