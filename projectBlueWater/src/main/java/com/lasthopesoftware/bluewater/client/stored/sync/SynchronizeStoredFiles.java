package com.lasthopesoftware.bluewater.client.stored.sync;

import io.reactivex.subjects.CompletableSubject;

public interface SynchronizeStoredFiles {
	CompletableSubject streamFileSynchronization();
}
