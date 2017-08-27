package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableMessageWriter;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellablePreparedMessengerOperator;

import java.util.concurrent.Executor;

public class QueuedPromise<Result> extends Promise<Result> {
	public QueuedPromise(MessengerOperator<Result> task, Executor executor) {
		super(new Execution.QueuedMessengerResponse<>(task, executor));
	}

	public QueuedPromise(CancellableMessageWriter<Result> task, Executor executor) {
		this(new CancellablePreparedMessengerOperator<>(task), executor);
	}

	public QueuedPromise(MessageWriter<Result> task, Executor executor) {
		this(new PreparedMessengerOperator<>(task), executor);
	}

	private static class Execution {
		static final class QueuedMessengerResponse<Result> implements
			MessengerOperator<Result>,
			Runnable {

			private final MessengerOperator<Result> task;
			private final Executor executor;
			private Messenger<Result> resultMessenger;

			QueuedMessengerResponse(MessengerOperator<Result> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void send(Messenger<Result> resultMessenger) {
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
