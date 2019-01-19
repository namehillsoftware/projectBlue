package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import com.namehillsoftware.handoff.promises.Promise;

public interface ProcessStoredFileJobs {
	Promise<StoredFileJobStatus> observeStoredFileDownload(StoredFileJob job);
}
