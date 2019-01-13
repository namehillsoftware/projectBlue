package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheDownloadFails;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static StoredFileJobException storedFileJobException;
	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static final List<StoredFileJobState> states = new ArrayList<>();

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200, new byte[0]));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> {
				final File file = mock(File.class);
				final File parentFile = mock(File.class);
				when(parentFile.mkdirs()).thenReturn(true);
				when(file.getParentFile()).thenReturn(parentFile);

				return file;
			},
			fakeConnectionProvider,
			mock(IStoredFileAccess.class),
			f -> new String[0],
			f -> false,
			f -> true,
			(is, f) -> { throw new IOException(); });

		try {
			for (StoredFileJobStatus status : storedFileJobProcessor.observeStoredFileDownload(
					new StoredFileJob(new ServiceFile(1), storedFile)).blockingIterable()) {
				states.add(status.storedFileJobState);
			}
		} catch (Throwable e) {
			if (e.getCause() instanceof StoredFileJobException)
				storedFileJobException = (StoredFileJobException)e.getCause();
		}
	}

	@Test
	public void thenTheStoredFileIsDownloading() {
		assertThat(states).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloading);
	}

	@Test
	public void thenAStoredFileJobExceptionIsThrown() {
		assertThat(storedFileJobException).isNotNull();
	}

	@Test
	public void thenTheInnerExceptionIsAnIoException() {
		assertThat(storedFileJobException.getCause()).isInstanceOf(IOException.class);
	}

	@Test
	public void thenTheStoredFileIsAssociatedWithTheException() {
		assertThat(storedFileJobException.getStoredFile()).isEqualTo(storedFile);
	}
}
