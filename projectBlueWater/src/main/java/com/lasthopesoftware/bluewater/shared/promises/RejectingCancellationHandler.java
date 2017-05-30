package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.Messenger;

import java.util.concurrent.CancellationException;

public class RejectingCancellationHandler implements Runnable {

	private final String cancellationMessage;
	private final Messenger messenger;

	private volatile boolean isCancelled;

	public RejectingCancellationHandler(String cancellationMessage, Messenger messenger) {
		this.cancellationMessage = cancellationMessage;
		this.messenger = messenger;
	}

	@Override
	public void run() {
		isCancelled = true;
		messenger.sendRejection(new CancellationException(cancellationMessage));
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}
