package com.lasthopesoftware.bluewater.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.shared.promises.WrappedCancellableExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedFunction;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.runnables.OneParameterAction;


public class DispatchedPromise<Result> extends Promise<Result> {
	public DispatchedPromise(OneParameterAction<Messenger<Result>> task, Context context) {
		super(new Executors.QueuedCancellableTask<>(task, new Handler(context.getMainLooper())));
	}

	public DispatchedPromise(OneParameterAction<Messenger<Result>> task, Handler handler) {
		super(new Executors.QueuedCancellableTask<>(task, handler));
	}

	public DispatchedPromise(CarelessFunction<Result> task, Context context) {
		super(new Executors.QueuedFunction<>(task, new Handler(context.getMainLooper())));
	}

	public DispatchedPromise(CarelessFunction<Result> task, Handler handler) {
		super(new Executors.QueuedFunction<>(task, handler));
	}

	private static class Executors {
		static class QueuedCancellableTask<Result> implements OneParameterAction<Messenger<Result>> {

			private final OneParameterAction<Messenger<Result>> task;
			private final Handler handler;

			QueuedCancellableTask(OneParameterAction<Messenger<Result>> task, Handler handler) {
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
	}
}
