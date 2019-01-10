package com.lasthopesoftware.bluewater.client.stored.sync;

import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import io.reactivex.Observable;

public interface SynchronizeStoredFiles {
	Observable<StoredFileJobStatus> streamFileSynchronization();
}
