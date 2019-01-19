package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import io.reactivex.Observable;

import java.util.Set;

public interface ProcessStoredFileJobs {
	Observable<StoredFileJobStatus> observeStoredFileDownload(Set<StoredFileJob> jobs);
}
