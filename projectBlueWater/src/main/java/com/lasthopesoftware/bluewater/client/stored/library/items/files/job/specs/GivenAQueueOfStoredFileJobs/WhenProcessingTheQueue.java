package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
		new StoredFile().setServiceId(5).setLibraryId(1),
		new StoredFile().setServiceId(7).setLibraryId(1),
		new StoredFile().setServiceId(114).setLibraryId(1),
		new StoredFile().setServiceId(92).setLibraryId(1)
	};

	private static final List<StoredFile> storedFilesMarkedAsDownloaded = new ArrayList<>();

	private static List<StoredFileJobStatus> storedFileStatuses;

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final IStoredFileAccess storedFileAccess = new IStoredFileAccess() {
			@Override
			public Promise<StoredFile> getStoredFile(int storedFileId) {
				return null;
			}

			@Override
			public Promise<StoredFile> getStoredFile(Library library, ServiceFile serviceServiceFile) {
				return null;
			}

			@Override
			public Promise<List<StoredFile>> getDownloadingStoredFiles() {
				return null;
			}

			@Override
			public Promise<StoredFile> markStoredFileAsDownloaded(StoredFile storedFile) {
				storedFilesMarkedAsDownloaded.add(storedFile);
				return new Promise<>(storedFile);
			}

			@Override
			public Promise<Void> addMediaFile(Library library, ServiceFile serviceFile, int mediaFileId, String filePath) {
				return null;
			}

			@Override
			public Promise<Void> pruneStoredFiles(Library library, Set<ServiceFile> serviceFilesToKeep) {
				return null;
			}
		};

		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200, new byte[0]));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			fakeConnectionProvider,
			storedFileAccess,
			f -> new String[0],
			f -> false,
			f -> true,
			(is, f) -> {});

		storedFileStatuses = storedFileJobProcessor.observeStoredFileDownload(storedFileJobs).toList().blockingGet();
	}

	@Test
	public void thenTheFilesAreAllMarkedAsDownloaded() {
		assertThat(storedFilesMarkedAsDownloaded).containsExactly(expectedStoredFiles);
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
}
