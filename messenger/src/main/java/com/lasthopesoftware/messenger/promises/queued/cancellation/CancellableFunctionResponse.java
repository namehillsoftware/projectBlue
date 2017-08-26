package com.lasthopesoftware.messenger.promises.queued.cancellation;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;

public final class CancellableFunctionResponse<Result> implements MessengerTask<Result> {
	private final CancellableMessageTask<Result> task;

	public CancellableFunctionResponse(CancellableMessageTask<Result> task) {
		this.task = task;
	}

	@Override
	public void execute(Messenger<Result> messenger) {
		final CancellationToken cancellationToken = new CancellationToken();
		messenger.cancellationRequested(cancellationToken);

		try {
			messenger.sendResolution(task.prepareMessage(cancellationToken));
		} catch (Throwable throwable) {
			messenger.sendRejection(throwable);
		}
	}
}
