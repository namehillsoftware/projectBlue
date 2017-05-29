package com.lasthopesoftware.promises;

public interface Messenger<Resolution> extends IRejectedPromise, IResolvedPromise<Resolution> {
	void cancellationRequested(Runnable response);
}
