package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job;

import io.reactivex.Observable;

public interface ProcessStoredFileJobs {
	Observable<StoredFileJobStatus> observeStoredFileDownload(StoredFileJob job);
}
