package com.lasthopesoftware.providers;


public class Cancellation {

	private boolean isCancelled;

	public void cancel() {
		isCancelled = true;
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}
