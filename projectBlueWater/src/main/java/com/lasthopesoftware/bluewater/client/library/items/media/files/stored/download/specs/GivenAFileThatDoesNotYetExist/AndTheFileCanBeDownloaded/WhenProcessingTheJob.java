package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResultOptions;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
	private static StoredFileJobResult result;

	@BeforeClass
	public static void before() throws StoredFileJobException, IOException {
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

		final IConnectionProvider fakeConnectionProvider = mock(IConnectionProvider.class);
		when(fakeConnectionProvider.getConnection(any())).thenReturn(connection);

		final StoredFileJob storedFileJob = new StoredFileJob(
			$ -> mock(File.class),
			fakeConnectionProvider,
			storedFileAccess,
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> false,
			f -> true,
			(is, f) -> {},
			new ServiceFile(1),
			storedFile);

		result = storedFileJob.processJob();
	}

	@Test
	public void thenTheFileIsMarkedAsDownloaded() {
		verify(storedFileAccess, times(1)).markStoredFileAsDownloaded(storedFile);
	}

	@Test
	public void thenTheJobResultIsDownloaded() {
		assertThat(result.storedFileJobResult).isEqualTo(StoredFileJobResultOptions.Downloaded);
	}
}
