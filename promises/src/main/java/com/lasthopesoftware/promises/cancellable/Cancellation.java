package com.lasthopesoftware.promises.cancellable;

/**
 * Created by david on 10/25/16.
 */

public abstract class Cancellation {
	private boolean isCancelled;

	public void cancel() {
		isCancelled = true;
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}
