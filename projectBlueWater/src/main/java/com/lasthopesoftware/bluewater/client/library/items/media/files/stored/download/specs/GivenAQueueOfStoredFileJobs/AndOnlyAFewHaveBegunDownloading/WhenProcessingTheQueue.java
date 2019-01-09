package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAQueueOfStoredFileJobs.AndOnlyAFewHaveBegunDownloading;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenProcessingTheQueue {

	private static final Queue<StoredFileJob> storedFileJobs = new LinkedList<>(Arrays.asList(
		new StoredFileJob(new ServiceFile(1), new StoredFile().setServiceId(1).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(2), new StoredFile().setServiceId(2).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(4), new StoredFile().setServiceId(4).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(5), new StoredFile().setServiceId(5).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(7), new StoredFile().setServiceId(7).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(114), new StoredFile().setServiceId(114).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(92), new StoredFile().setServiceId(92).setLibraryId(1))));

	private static final StoredFile[] expectedDownloadingStoredFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
		new StoredFile().setServiceId(5).setLibraryId(1),
		new StoredFile().setServiceId(114).setLibraryId(1),
		new StoredFile().setServiceId(7).setLibraryId(1)
	};

	private static final StoredFile[] expectedStoredFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
		new StoredFile().setServiceId(5).setLibraryId(1),
		new StoredFile().setServiceId(114).setLibraryId(1)
	};

	private static final List<StoredFile> downloadingStoredFiles = new ArrayList<>();
	private static final List<StoredFileJobStatus> downloadedStoredFiles = new ArrayList<>();

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(
			job -> {
				if (Arrays.asList(expectedStoredFiles).contains(job.getStoredFile()))
					return Observable.just(
						new StoredFileJobStatus(
							mock(File.class),
							job.getStoredFile(),
							StoredFileJobState.Downloading),
						new StoredFileJobStatus(
							mock(File.class),
							job.getStoredFile(),
							StoredFileJobState.Downloaded));

				if (Arrays.asList(expectedDownloadingStoredFiles).contains(job.getStoredFile()))
					return Observable.just(
						new StoredFileJobStatus(
							mock(File.class),
							job.getStoredFile(),
							StoredFileJobState.Downloading));

				return Observable.empty();
			});

		storedFileDownloader.setOnFileDownloading(downloadingStoredFiles::add);
		storedFileDownloader.process(storedFileJobs).blockingIterable().forEach(downloadedStoredFiles::add);
	}

	@Test
	public void thenTheFilesAreAllMarkedAsSuccessfullyDownload() {
		assertThat(Stream.of(downloadedStoredFiles).map(r -> r.storedFileJobState).distinct().single())
			.isEqualTo(StoredFileJobState.Downloaded);
	}

	@Test
	public void thenTheFilesAreBroadcastAsDownloading() {
		assertThat(downloadingStoredFiles).containsOnly(expectedDownloadingStoredFiles);
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(downloadedStoredFiles).map(r -> r.storedFile).toList())
			.containsOnly(expectedStoredFiles);
	}
}
