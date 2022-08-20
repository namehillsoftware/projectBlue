package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.WithParentDirectory.ThatCannotBeCreated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new LibraryId(7), 1, new ServiceFile(1), "test-path", true);
	private static StorageCreatePathException storageCreatePathException;

	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> {
				final File file = mock(File.class);
				final File parentFile = mock(File.class);
				when(parentFile.mkdirs()).thenReturn(false);
				when(file.getParentFile()).thenReturn(parentFile);

				return file;
			},
			mock(AccessStoredFiles.class),
			(libraryId, f) -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> false,
			f -> true,
			(is, f) -> {});

		try {
			storedFileJobProcessor.observeStoredFileDownload(Collections.singleton(
				new StoredFileJob(new LibraryId(7), new ServiceFile(1), storedFile))).blockingSubscribe();
		} catch (Throwable e) {
			if (e.getCause() instanceof StorageCreatePathException)
				storageCreatePathException = (StorageCreatePathException)e.getCause();
		}
	}

	@Test
	public void thenAStorageCreatePathExceptionIsThrown() {
		assertThat(storageCreatePathException).isNotNull();
	}
}
