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


public class LoopedInPromise<Result> extends Promise<Result> {
	public LoopedInPromise(CarelessFunction<Result> task, Context context) {
		this(task, new Handler(context.getMainLooper()));
	}

	public LoopedInPromise(CarelessFunction<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new FunctionResponse<>(task), handler));
	}

	public LoopedInPromise(CarelessOneParameterFunction<CancellationToken, Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new CancellableFunctionResponse<>(task), handler));
	}

	private static class Executors {
		static final class LoopedInResponse<Result> implements OneParameterAction<Messenger<Result>>, Runnable {

			private final OneParameterAction<Messenger<Result>> task;
			private final Handler handler;
			private Messenger<Result> resultMessenger;

			LoopedInResponse(OneParameterAction<Messenger<Result>> task, Handler handler) {
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
