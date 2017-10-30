package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableMessageWriter;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellablePreparedMessengerOperator;

import java.util.concurrent.Executor;

public class QueuedPromise<Resolution> extends Promise<Resolution> {
	public QueuedPromise(MessengerOperator<Resolution> task, Executor executor) {
		super(new Execution.QueuedMessengerResponse<Resolution>(task, executor));
	}

	public QueuedPromise(CancellableMessageWriter<Resolution> task, Executor executor) {
		this(new CancellablePreparedMessengerOperator<Resolution>(task), executor);
	}

	public QueuedPromise(MessageWriter<Resolution> task, Executor executor) {
		this(new PreparedMessengerOperator<Resolution>(task), executor);
	}

	private static class Execution {
		static final class QueuedMessengerResponse<Resolution> implements
			MessengerOperator<Resolution>,
			Runnable {

			private final MessengerOperator<Resolution> task;
			private final Executor executor;
			private Messenger<Resolution> resultMessenger;

			QueuedMessengerResponse(MessengerOperator<Resolution> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void send(Messenger<Resolution> resultMessenger) {
				this.resultMessenger = resultMessenger;
				executor.execute(this);
			}

			@Override
			public void run() {
				task.send(resultMessenger);
			}
		}
	}
}
