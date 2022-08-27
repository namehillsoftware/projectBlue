package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.AndSomeAreAlreadyDownloaded.AndOneCannotBeRead;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.read.exceptions.StorageReadFileException;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheQueue {

	private static final Set<StoredFileJob> storedFileJobs = new HashSet<>(Arrays.asList(
		new StoredFileJob(new LibraryId(1), new ServiceFile(1), new StoredFile().setServiceId(1).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(2), new StoredFile().setServiceId(2).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(4), new StoredFile().setServiceId(4).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(5), new StoredFile().setServiceId(5).setLibraryId(1).setIsDownloadComplete(true)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(7), new StoredFile().setServiceId(7).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(114), new StoredFile().setServiceId(114).setLibraryId(1).setIsDownloadComplete(true)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(92), new StoredFile().setServiceId(92).setLibraryId(1).setIsDownloadComplete(true))));

	private static final StoredFile[] expectedStoredFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(2).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
		new StoredFile().setServiceId(7).setLibraryId(1),
	};

	private static final MarkedFilesStoredFileAccess storedFilesAccess = new MarkedFilesStoredFileAccess();

	private static final List<StoredFileJobStatus> storedFileStatuses = new ArrayList<>();
	private static StorageReadFileException exception;

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			storedFile -> {
				final File file = mock(File.class);

				when(file.exists()).thenReturn(storedFile.isDownloadComplete());

				if (storedFile.getServiceId() == 114)
					when(file.getPath()).thenReturn("unreadable");

				return file;
			},
			storedFilesAccess,
			(libraryId, f) -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> !"unreadable".equals(f.getPath()),
			f -> true,
			(is, f) -> {});

		storedFileJobProcessor
			.observeStoredFileDownload(storedFileJobs)
			.blockingSubscribe(
				storedFileStatuses::add,
				error -> {
					if (error instanceof StorageReadFileException)
						exception = (StorageReadFileException)error;
				});
	}

	@Test
	public void thenNoErrorOccurs() {
		assertThat(exception).isNull();
	}

	@Test
	public void thenTheCorrectFilesAreMarkedUnreadable() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Unreadable).single().storedFile.getServiceId()).isEqualTo(114);
	}

	@Test
	public void thenTheCorrectFilesAreMarkedAsDownloaded() {
		assertThat(storedFilesAccess.storedFilesMarkedAsDownloaded).containsExactly(expectedStoredFiles);
	}

	@Test
	public void thenTheFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloading)
			.map(r -> r.storedFile).toList()).containsExactly(expectedStoredFiles);
	}

	@Test
	public void thenTheCorrectFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloaded)
			.map(r -> r.storedFile).toList()).isSubsetOf(
				Stream.of(storedFileJobs)
					.map(StoredFileJob::getStoredFile)
					.filter(f -> f.getServiceId() != 114)
					.toList());
	}
}
