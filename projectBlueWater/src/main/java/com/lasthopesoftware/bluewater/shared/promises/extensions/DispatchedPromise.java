package com.lasthopesoftware.bluewater.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.shared.promises.WrappedCancellableExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedFunction;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;


public class DispatchedPromise<Result> extends Promise<Result> {
	public DispatchedPromise(OneParameterAction<Messenger<Result>> task, Context context) {
		super(new Executors.QueuedMessengerTask<>(task, new Handler(context.getMainLooper())));
	}

	public DispatchedPromise(OneParameterAction<Messenger<Result>> task, Handler handler) {
		super(new Executors.QueuedMessengerTask<>(task, handler));
	}

	public DispatchedPromise(CarelessFunction<Result> task, Context context) {
		super(new Executors.QueuedFunction<>(task, new Handler(context.getMainLooper())));
	}

	public DispatchedPromise(CarelessFunction<Result> task, Handler handler) {
		super(new Executors.QueuedFunction<>(task, handler));
	}

	public DispatchedPromise(CarelessOneParameterFunction<CancellationToken, Result> task, Handler handler) {
		super(new Executors.QueuedMessengerTask<>(new Executors.QueuedCancellableFunctionExecutor<>(task), handler));
	}

	private static class Executors {
		static class QueuedMessengerTask<Result> implements OneParameterAction<Messenger<Result>> {

			private final OneParameterAction<Messenger<Result>> task;
			private final Handler handler;

			QueuedMessengerTask(OneParameterAction<Messenger<Result>> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void runWith(Messenger<Result> resultMessenger) {
				this.handler.post(new WrappedCancellableExecutor<>(resultMessenger, task));
			}
		}

		static class QueuedFunction<Result> implements OneParameterAction<Messenger<Result>> {

			private final CarelessFunction<Result> callable;
			private final Handler handler;

			QueuedFunction(CarelessFunction<Result> callable, Handler handler) {
				this.callable = callable;
				this.handler = handler;
			}

			@Override
			public void runWith(Messenger<Result> resultMessenger) {
				this.handler.post(new WrappedFunction<>(callable, resultMessenger));
			}
		}

		static class QueuedCancellableFunctionExecutor<Result> implements OneParameterAction<Messenger<Result>> {
			private final CarelessOneParameterFunction<CancellationToken, Result> task;

			QueuedCancellableFunctionExecutor(CarelessOneParameterFunction<CancellationToken, Result> task) {
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
