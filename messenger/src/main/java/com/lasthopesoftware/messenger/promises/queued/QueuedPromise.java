package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableImmediateMessage;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableMessageTask;

import java.util.concurrent.Executor;

public class QueuedPromise<Result> extends Promise<Result> {
	public QueuedPromise(MessengerTask<Result> task, Executor executor) {
		super(new Execution.QueuedMessengerResponse<>(task, executor));
	}

	public QueuedPromise(CancellableMessageTask<Result> task, Executor executor) {
		this(new CancellableImmediateMessage<>(task), executor);
	}

	public QueuedPromise(MessageTask<Result> task, Executor executor) {
		this(new ImmediateMessage<>(task), executor);
	}

	private static class Execution {
		static final class QueuedMessengerResponse<Result> implements
			MessengerTask<Result>,
			Runnable {

			private final MessengerTask<Result> task;
			private final Executor executor;
			private Messenger<Result> resultMessenger;

			QueuedMessengerResponse(MessengerTask<Result> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void execute(Messenger<Result> resultMessenger) {
				this.resultMessenger = resultMessenger;
				executor.execute(this);
			}

			@Override
			public void run() {
				task.execute(resultMessenger);
			}
		}
	}
}
