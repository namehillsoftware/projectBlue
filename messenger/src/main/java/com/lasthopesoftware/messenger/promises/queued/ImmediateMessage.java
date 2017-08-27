package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;

public final class ImmediateMessage<Result> implements MessengerOperator<Result> {

	private final MessageTask<Result> task;

	public ImmediateMessage(MessageTask<Result> task) {
		this.task = task;
	}

	@Override
	public void send(Messenger<Result> messenger) {
		try {
			messenger.sendResolution(task.prepareMessage());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
