package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

public final class StoredFileDownloader implements IStoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);
	@NonNull
	private final ProcessStoredFileJobs storedFileJobs;

	public StoredFileDownloader(@NonNull ProcessStoredFileJobs storedFileJobs) {
		this.storedFileJobs = storedFileJobs;
	}

	@Override
	public Observable<StoredFileJobStatus> process(Queue<StoredFileJob> jobsQueue) {
		return Observable.fromIterable(jobsQueue).flatMap(this::processStoredFileJob);
	}

	private Observable<StoredFileJobStatus> processStoredFileJob(StoredFileJob storedFileJob) {
		return storedFileJobs
			.observeStoredFileDownload(storedFileJob);
	}
}
