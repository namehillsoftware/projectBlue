package com.lasthopesoftware.bluewater.client.sync.library.items.files.job;

import io.reactivex.Observable;

public interface ProcessStoredFileJobs {
	Observable<StoredFileJobStatus> observeStoredFileDownload(StoredFileJob job);
}
