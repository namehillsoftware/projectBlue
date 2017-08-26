package com.lasthopesoftware.messenger.promises.queued.cancellation;

public interface CancellableMessageTask<Resolution> {
	Resolution prepareMessage(CancellationToken cancellationToken) throws Throwable;
}
