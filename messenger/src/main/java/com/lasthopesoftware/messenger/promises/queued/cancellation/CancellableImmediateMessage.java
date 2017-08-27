package com.lasthopesoftware.messenger.promises.queued.cancellation;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;

public final class CancellableImmediateMessage<Result> implements MessengerOperator<Result> {
	private final CancellableMessageTask<Result> task;

	public CancellableImmediateMessage(CancellableMessageTask<Result> task) {
		this.task = task;
	}

	@Override
	public void send(Messenger<Result> messenger) {
		final CancellationToken cancellationToken = new CancellationToken();
		messenger.cancellationRequested(cancellationToken);

		try {
			messenger.sendResolution(task.prepareMessage(cancellationToken));
		} catch (Throwable throwable) {
			messenger.sendRejection(throwable);
		}
	}
}
