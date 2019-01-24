package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs.AndAnUnexpectedTransferErrorOccurs;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheQueue {

	private static final Set<StoredFileJob> storedFileJobs = new HashSet<>(Arrays.asList(
		new StoredFileJob(new ServiceFile(1), new StoredFile().setServiceId(1).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(2), new StoredFile().setServiceId(2).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(4), new StoredFile().setServiceId(4).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(5), new StoredFile().setServiceId(5).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(7), new StoredFile().setServiceId(7).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(114), new StoredFile().setServiceId(114).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(92), new StoredFile().setServiceId(92).setLibraryId(1))));

	private static final StoredFile[] expectedStoredFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(2).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
	};

	private static final MarkedFilesStoredFileAccess storedFilesAccess = new MarkedFilesStoredFileAccess();

	private static final List<StoredFileJobStatus> storedFileStatuses = new ArrayList<>();
	private static StoredFileJobException exception;

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			storedFile -> {
				final File file = mock(File.class);

				when(file.exists()).thenReturn(storedFile.isDownloadComplete());

				if (storedFile.getServiceId() == 5)
					when(file.getPath()).thenReturn("write-failure");

				return file;
			},
			storedFilesAccess,
			f -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> true,
			f -> true,
			(is, f) -> {
				if ("write-failure".equals(f.getPath()))
					throw new UnexpectedException();
			});

			storedFileJobProcessor.observeStoredFileDownload(storedFileJobs).blockingSubscribe(
				storedFileStatuses::add,
				e -> {
					if (e instanceof StoredFileJobException)
						exception = (StoredFileJobException)e;
				});
		}

	@Test
	public void thenTheErrorFileIsCorrect() {
		assertThat(exception.getStoredFile().getServiceId()).isEqualTo(5);
	}

	@Test
	public void thenTheUnexpectedEsceptionIsCorrect() {
		assertThat(exception.getCause()).isInstanceOf(UnexpectedException.class);
	}

	@Test
	public void thenTheFilesAreMarkedAsDownloaded() {
		assertThat(storedFilesAccess.storedFilesMarkedAsDownloaded).containsExactly(expectedStoredFiles);
	}

	@Test
	public void thenTheFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloading)
			.map(r -> r.storedFile.getServiceId()).toList())
			.containsExactly(1, 2, 4, 5);
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloaded)
			.map(r -> r.storedFile).toList()).containsExactly(expectedStoredFiles);
	}

	private static class UnexpectedException extends RuntimeException {}
}
