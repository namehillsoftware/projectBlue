package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.lasthopesoftware.bluewater.shared.promises.WrappedCancellableExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedFunction;
import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class QueuedPromise<Result> extends Promise<Result> {
	public QueuedPromise(OneParameterAction<Messenger<Result>> task, Executor executor) {
		super(new Executors.QueuedCancellableTask<>(task, executor));
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
	}
}
