package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;

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
