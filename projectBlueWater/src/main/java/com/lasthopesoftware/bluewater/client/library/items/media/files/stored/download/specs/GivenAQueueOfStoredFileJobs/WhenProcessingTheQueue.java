package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAQueueOfStoredFileJobs;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResultOptions;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

	private static Collection<StoredFileJobResult> storedFileJobResults;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(
			job -> new Promise<>(new StoredFileJobResult(
				mock(File.class),
				job.getStoredFile(),
				StoredFileJobResultOptions.Downloaded)));

		storedFileJobResults = new FuturePromise<>(storedFileDownloader.process(storedFileJobs)).get();
	}

	@Test
	public void thenTheFilesSuccessfullyDownload() {
		assertThat(Stream.of(storedFileJobResults).map(r -> r.storedFileJobResult).distinct().single())
			.isEqualTo(StoredFileJobResultOptions.Downloaded);
	}
}
