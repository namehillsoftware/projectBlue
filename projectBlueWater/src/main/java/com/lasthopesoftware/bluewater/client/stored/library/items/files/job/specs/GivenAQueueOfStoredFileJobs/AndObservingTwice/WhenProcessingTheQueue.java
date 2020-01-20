package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs.AndObservingTwice;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenProcessingTheQueue {

	private static final Set<StoredFileJob> storedFileJobs = new HashSet<>(Arrays.asList(
		new StoredFileJob(new LibraryId(1), new ServiceFile(1), new StoredFile().setServiceId(1).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(2), new StoredFile().setServiceId(2).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(4), new StoredFile().setServiceId(4).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(5), new StoredFile().setServiceId(5).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(7), new StoredFile().setServiceId(7).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(114), new StoredFile().setServiceId(114).setLibraryId(1)),
		new StoredFileJob(new LibraryId(1), new ServiceFile(92), new StoredFile().setServiceId(92).setLibraryId(1))));

	private static final StoredFile[] expectedStoredFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(2).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
		new StoredFile().setServiceId(5).setLibraryId(1),
		new StoredFile().setServiceId(7).setLibraryId(1),
		new StoredFile().setServiceId(114).setLibraryId(1),
		new StoredFile().setServiceId(92).setLibraryId(1)
	};

	private static final MarkedFilesStoredFileAccess storedFilesAccess = new MarkedFilesStoredFileAccess();

	private static List<StoredFileJobStatus> storedFileStatuses;
	private static List<StoredFileJobStatus> secondStoredFileStatuses;

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			storedFilesAccess,
			(libraryId, f) -> new Promise<>(new ByteArrayInputStream(new byte[0])),
			f -> false,
			f -> true,
			(is, f) -> {});

		final Single<List<StoredFileJobStatus>> observedStatuses = storedFileJobProcessor.observeStoredFileDownload(storedFileJobs).toList();
		storedFileStatuses = observedStatuses.blockingGet();
		secondStoredFileStatuses = observedStatuses.blockingGet();
	}

	@Test
	public void thenTheFilesAreAllMarkedAsDownloaded() {
		assertThat(storedFilesAccess.storedFilesMarkedAsDownloaded).containsExactly(expectedStoredFiles);
	}

	@Test
	public void thenTheFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloading)
			.map(r -> r.storedFile).toList()).containsExactly(expectedStoredFiles);
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloaded)
			.map(r -> r.storedFile).toList()).containsOnly(expectedStoredFiles);
	}

	@Test
	public void thenTheSecondListOfStoredFilesIsEmpty() {
		assertThat(secondStoredFileStatuses).isEmpty();
	}
}
