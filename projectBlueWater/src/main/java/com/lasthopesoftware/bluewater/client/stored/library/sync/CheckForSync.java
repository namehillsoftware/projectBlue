package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.namehillsoftware.handoff.promises.Promise;

public interface CheckForSync {
	Promise<Boolean> promiseIsSyncNeeded();
}
