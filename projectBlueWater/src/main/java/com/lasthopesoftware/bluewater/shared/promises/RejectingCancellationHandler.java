package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.IRejectedPromise;

import java.util.concurrent.CancellationException;

/**
 * Created by david on 2/28/17.
 */

public class RejectingCancellationHandler implements Runnable {

	private final String cancellationMessage;
	private final IRejectedPromise reject;

	private volatile boolean isCancelled;

	public RejectingCancellationHandler(String cancellationMessage, IRejectedPromise reject) {
		this.cancellationMessage = cancellationMessage;
		this.reject = reject;
	}

	@Override
	public void run() {
		isCancelled = true;
		this.reject.sendRejection(new CancellationException(cancellationMessage));
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}
