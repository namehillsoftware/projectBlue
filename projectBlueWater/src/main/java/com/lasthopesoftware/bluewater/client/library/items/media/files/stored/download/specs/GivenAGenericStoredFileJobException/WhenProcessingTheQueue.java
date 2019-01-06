package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAGenericStoredFileJobException;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheQueue {

	private static final Queue<StoredFileJob> storedFileJobs = new LinkedList<>(Arrays.asList(
		new StoredFileJob(new ServiceFile(1), new StoredFile().setLibraryId(4).setServiceId(1)),
		new StoredFileJob(new ServiceFile(2), new StoredFile().setLibraryId(4).setServiceId(2)),
		new StoredFileJob(new ServiceFile(4), new StoredFile().setLibraryId(4).setServiceId(4)),
		new StoredFileJob(new ServiceFile(5), new StoredFile().setLibraryId(4).setServiceId(5)),
		new StoredFileJob(new ServiceFile(7), new StoredFile().setLibraryId(4).setServiceId(7)),
		new StoredFileJob(new ServiceFile(114), new StoredFile().setLibraryId(4).setServiceId(114)),
		new StoredFileJob(new ServiceFile(92), new StoredFile().setLibraryId(4).setServiceId(92))));

	private static final Queue<StoredFile> expectedStoredFileJobs = new LinkedList<>(Arrays.asList(
		new StoredFile().setLibraryId(4).setServiceId(1),
		new StoredFile().setLibraryId(4).setServiceId(4),
		new StoredFile().setLibraryId(4).setServiceId(5),
		new StoredFile().setLibraryId(4).setServiceId(114),
		new StoredFile().setLibraryId(4).setServiceId(92)));

	private static final List<StoredFile> downloadingStoredFiles = new ArrayList<>();
	private static final List<StoredFileJobStatus> downloadedStoredFiles = new ArrayList<>();

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final ProcessStoredFileJobs storedFileJobs = mock(ProcessStoredFileJobs.class);
		when(storedFileJobs.observeStoredFileDownload(any()))
			.thenAnswer(a -> Observable.just(new StoredFileJobStatus(
				mock(File.class),
				a.<StoredFileJob>getArgument(0).getStoredFile(),
				StoredFileJobState.Downloaded)));
		when(storedFileJobs.observeStoredFileDownload(new StoredFileJob(new ServiceFile(7), new StoredFile().setLibraryId(4).setServiceId(7))))
			.thenAnswer(a -> Observable.error(new StoredFileJobException(a.<StoredFileJob>getArgument(0).getStoredFile(), new Exception())));
		when(storedFileJobs.observeStoredFileDownload(new StoredFileJob(new ServiceFile(2), new StoredFile().setLibraryId(4).setServiceId(2))))
			.thenAnswer(a -> Observable.error(new StoredFileJobException(a.<StoredFileJob>getArgument(0).getStoredFile(), new Exception())));

		final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(storedFileJobs);

		storedFileDownloader.setOnFileDownloading(downloadingStoredFiles::add);
		storedFileDownloader.process(WhenProcessingTheQueue.storedFileJobs).blockingIterable().forEach(downloadedStoredFiles::add);
	}

	@Test
	public void thenTheCorrectFilesAreMarkedAsSuccessfullyDownload() {
		assertThat(Stream.of(downloadedStoredFiles).map(r -> r.storedFileJobState).distinct().single())
			.isEqualTo(StoredFileJobState.Downloaded);
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloading() {
		assertThat(downloadingStoredFiles).containsOnlyElementsOf(Stream.of(storedFileJobs).map(StoredFileJob::getStoredFile).toList());
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(downloadedStoredFiles).map(r -> r.storedFile).toList())
			.containsOnlyElementsOf(expectedStoredFileJobs);
	}
}
