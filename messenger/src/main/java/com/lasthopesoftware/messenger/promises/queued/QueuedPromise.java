package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableFunctionResponse;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.Executor;

public class QueuedPromise<Result> extends Promise<Result> {
	public QueuedPromise(OneParameterAction<Messenger<Result>> task, Executor executor) {
		super(new Execution.QueuedMessengerResponse<>(task, executor));
	}

	public QueuedPromise(CarelessOneParameterFunction<CancellationToken, Result> task, Executor executor) {
		this(new CancellableFunctionResponse<>(task), executor);
	}

	public QueuedPromise(CarelessFunction<Result> task, Executor executor) {
		this(new FunctionResponse<>(task), executor);
	}

	private static class Execution {
		static final class QueuedMessengerResponse<Result> implements
			OneParameterAction<Messenger<Result>>,
			Runnable {

			private final OneParameterAction<Messenger<Result>> task;
			private final Executor executor;
			private Messenger<Result> resultMessenger;

			QueuedMessengerResponse(OneParameterAction<Messenger<Result>> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void runWith(Messenger<Result> resultMessenger) {
				this.resultMessenger = resultMessenger;
				executor.execute(this);
			}

			@Override
			public void run() {
				task.runWith(resultMessenger);
			}
		}
	}
}
