package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.namehillsoftware.handoff.promises.Promise;

public interface ProcessStoredFileJobs {
	Promise<StoredFileJobResult> promiseDownloadedStoredFile(StoredFileJob job);
}
