package com.namehillsoftware.handoff.promises.queued.cancellation;

public interface CancellableMessageWriter<Resolution> {
	Resolution prepareMessage(CancellationToken cancellationToken) throws Throwable;
}
