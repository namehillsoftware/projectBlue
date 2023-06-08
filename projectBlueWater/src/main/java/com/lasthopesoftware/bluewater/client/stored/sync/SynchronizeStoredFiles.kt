package com.lasthopesoftware.bluewater.client.stored.sync;

import io.reactivex.Completable;

public interface SynchronizeStoredFiles {
	Completable streamFileSynchronization();
}
