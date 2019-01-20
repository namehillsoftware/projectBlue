package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheDownloadFails;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static StoredFileWriteException storedFileWriteException;
	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static List<StoredFileJobState> states = new ArrayList<>();

	@RequiresApi(api = Build.VERSION_CODES.N)
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
			mock(IStoredFileAccess.class),
			f -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> false,
			f -> true,
			(is, f) -> { throw new IOException(); });

		storedFileJobProcessor.observeStoredFileDownload(
			Collections.singleton(new StoredFileJob(new ServiceFile(1), storedFile)))
			.map(f -> f.storedFileJobState)
			.blockingSubscribe(
				storedFileJobState -> states.add(storedFileJobState),
				e -> {
					if (e instanceof StoredFileWriteException)
						storedFileWriteException = (StoredFileWriteException)e;
				});
	}

	@Test
	public void thenTheStoredFileIsDownloading() {
		assertThat(states).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloading);
	}

	@Test
	public void thenAStoredFileJobExceptionIsThrown() {
		assertThat(storedFileWriteException).isNotNull();
	}

	@Test
	public void thenTheInnerExceptionIsAnIoException() {
		assertThat(storedFileWriteException.getCause()).isInstanceOf(IOException.class);
	}

	@Test
	public void thenTheStoredFileIsAssociatedWithTheException() {
		assertThat(storedFileWriteException.getStoredFile()).isEqualTo(storedFile);
	}
}
