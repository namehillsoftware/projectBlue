package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import io.reactivex.Observable;

public interface ProcessStoredFileJobs {
	Observable<StoredFileJobStatus> observeStoredFileDownload(StoredFileJob job);
}
