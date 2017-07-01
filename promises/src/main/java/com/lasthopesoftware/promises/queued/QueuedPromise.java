package com.lasthopesoftware.promises.queued;

import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.Executor;

public class QueuedPromise<Result> extends Promise<Result> {
	public QueuedPromise(OneParameterAction<Messenger<Result>> task, Executor executor) {
		super(new Executors.QueuedCancellableTask<>(task, executor));
	}

	public QueuedPromise(CarelessOneParameterFunction<CancellationToken, Result> task, Executor executor) {
		this((new Executors.CancellableFunctionExecutor<>(task)), executor);
	}

	public QueuedPromise(CarelessFunction<Result> task, Executor executor) {
		super(new Executors.QueuedFunction<>(task, executor));
	}

	private static class Executors {
		static class QueuedCancellableTask<Result> implements OneParameterAction<Messenger<Result>> {

			private final OneParameterAction<Messenger<Result>> task;
			private final Executor executor;

			QueuedCancellableTask(OneParameterAction<Messenger<Result>> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void runWith(Messenger<Result> resultMessenger) {
				this.executor.execute(new WrappedCancellableExecutor<>(resultMessenger, task));
			}
		}

		static class QueuedFunction<Result> implements OneParameterAction<Messenger<Result>> {

			private final CarelessFunction<Result> callable;
			private final Executor executor;

			QueuedFunction(CarelessFunction<Result> callable, Executor executor) {
				this.callable = callable;
				this.executor = executor;
			}

			@Override
			public void runWith(Messenger<Result> resultMessenger) {
				this.executor.execute(new WrappedFunction<>(callable, resultMessenger));
			}
		}

		private static class CancellableFunctionExecutor<Result> implements OneParameterAction<Messenger<Result>> {
			private final CarelessOneParameterFunction<CancellationToken, Result> task;

			CancellableFunctionExecutor(CarelessOneParameterFunction<CancellationToken, Result> task) {
				this.task = task;
			}

			@Override
			public void runWith(Messenger<Result> messenger) {
				final CancellationToken cancellationToken = new CancellationToken();
				messenger.cancellationRequested(cancellationToken);

				try {
					messenger.sendResolution(task.resultFrom(cancellationToken));
				} catch (Throwable throwable) {
					messenger.sendRejection(throwable);
				}
			}
		}
	}
}
