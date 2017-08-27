package com.lasthopesoftware.messenger.promises.queued.cancellation;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;

public final class CancellablePreparedMessengerOperator<Result> implements MessengerOperator<Result> {
	private final CancellableMessageWriter<Result> writer;

	public CancellablePreparedMessengerOperator(CancellableMessageWriter<Result> writer) {
		this.writer = writer;
	}

	@Override
	public void send(Messenger<Result> messenger) {
		final CancellationToken cancellationToken = new CancellationToken();
		messenger.cancellationRequested(cancellationToken);

		try {
			messenger.sendResolution(writer.prepareMessage(cancellationToken));
		} catch (Throwable throwable) {
			messenger.sendRejection(throwable);
		}
	}
}
