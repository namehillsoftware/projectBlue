package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import io.reactivex.Observable;

import java.util.Queue;

public interface IStoredFileDownloader {
	Observable<StoredFileJobStatus> process(Queue<StoredFileJob> jobsQueue);
}
