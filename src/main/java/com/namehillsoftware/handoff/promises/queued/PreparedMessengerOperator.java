package com.namehillsoftware.handoff.promises.queued;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;

public final class PreparedMessengerOperator<Result> implements MessengerOperator<Result> {

	private final MessageWriter<Result> writer;

	public PreparedMessengerOperator(MessageWriter<Result> writer) {
		this.writer = writer;
	}

	@Override
	public void send(Messenger<Result> messenger) {
		try {
			messenger.sendResolution(writer.prepareMessage());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
