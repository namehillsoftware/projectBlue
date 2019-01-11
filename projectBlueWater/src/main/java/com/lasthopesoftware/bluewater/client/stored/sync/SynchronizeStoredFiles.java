package com.lasthopesoftware.bluewater.client.stored.sync;

import androidx.work.ListenableWorker;
import io.reactivex.Single;

public interface SynchronizeStoredFiles {
	Single<ListenableWorker.Result> streamFileSynchronization();
}
