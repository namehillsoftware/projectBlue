package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAQueueOfStoredFileJobs;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResultOptions;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenProcessingTheQueue {

	private static final Queue<StoredFileJob> storedFileJobs = new LinkedList<>(Arrays.asList(
		new StoredFileJob(new ServiceFile(1), new StoredFile()),
		new StoredFileJob(new ServiceFile(2), new StoredFile()),
		new StoredFileJob(new ServiceFile(4), new StoredFile()),
		new StoredFileJob(new ServiceFile(5), new StoredFile()),
		new StoredFileJob(new ServiceFile(7), new StoredFile()),
		new StoredFileJob(new ServiceFile(114), new StoredFile()),
		new StoredFileJob(new ServiceFile(92), new StoredFile())));

	private static final List<StoredFile> downloadingStoredFiles = new ArrayList<>();
	private static final List<StoredFileJobResult> downloadedStoredFiles = new ArrayList<>();

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(
			job -> new Promise<>(new StoredFileJobResult(
				mock(File.class),
				job.getStoredFile(),
				StoredFileJobResultOptions.Downloaded)));

		storedFileDownloader.setOnFileDownloading(downloadingStoredFiles::add);
		storedFileDownloader.process(storedFileJobs).blockingIterable().forEach(downloadedStoredFiles::add);
	}

	@Test
	public void thenTheFilesAreAllMarkedAsSuccessfullyDownload() {
		assertThat(Stream.of(downloadedStoredFiles).map(r -> r.storedFileJobResult).distinct().single())
			.isEqualTo(StoredFileJobResultOptions.Downloaded);
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloading() {
		assertThat(downloadingStoredFiles).containsOnlyElementsOf(Stream.of(storedFileJobs).map(StoredFileJob::getStoredFile).toList());
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(downloadedStoredFiles).map(r -> r.storedFile).toList())
			.containsOnlyElementsOf(Stream.of(storedFileJobs).map(StoredFileJob::getStoredFile).toList());
	}
}
