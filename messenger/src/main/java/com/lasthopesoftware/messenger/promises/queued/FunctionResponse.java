package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;

public final class FunctionResponse<Result> implements MessengerTask<Result> {

	private final MessageTask<Result> task;

	public FunctionResponse(MessageTask<Result> task) {
		this.task = task;
	}

	@Override
	public void execute(Messenger<Result> messenger) {
		try {
			messenger.sendResolution(task.prepareMessage());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
