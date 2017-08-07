package com.lasthopesoftware.bluewater.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.FunctionResponse;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableFunctionResponse;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;


public class ContextSafePromise<Result> extends Promise<Result> {
	public ContextSafePromise(CarelessFunction<Result> task, Context context) {
		this(task, new Handler(context.getMainLooper()));
	}

	public ContextSafePromise(CarelessFunction<Result> task, Handler handler) {
		super(new Executors.QueuedMessengerTask<>(new FunctionResponse<>(task), handler));
	}

	public ContextSafePromise(CarelessOneParameterFunction<CancellationToken, Result> task, Handler handler) {
		super(new Executors.QueuedMessengerTask<>(new CancellableFunctionResponse<>(task), handler));
	}

	private static class Executors {
		static class QueuedMessengerTask<Result> implements OneParameterAction<Messenger<Result>>, Runnable {

			private final OneParameterAction<Messenger<Result>> task;
			private final Handler handler;
			private Messenger<Result> resultMessenger;

			QueuedMessengerTask(OneParameterAction<Messenger<Result>> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void runWith(Messenger<Result> resultMessenger) {
				this.resultMessenger = resultMessenger;

				if (handler.getLooper().getThread() == Thread.currentThread())
					run();
				else
					handler.post(this);
			}

			@Override
			public void run() {
				task.runWith(resultMessenger);
			}
		}
	}
}
