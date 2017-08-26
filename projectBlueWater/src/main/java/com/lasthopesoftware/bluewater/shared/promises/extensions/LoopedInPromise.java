package com.lasthopesoftware.bluewater.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.FunctionResponse;
import com.lasthopesoftware.messenger.promises.queued.MessageTask;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableFunctionResponse;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableMessageTask;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

public class LoopedInPromise<Result> extends Promise<Result> {

	public LoopedInPromise(MessageTask<Result> task, Context context) {
		this(task, new Handler(context.getMainLooper()));
	}

	public LoopedInPromise(MessageTask<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new FunctionResponse<>(task), handler));
	}

	public LoopedInPromise(CancellableMessageTask<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new CancellableFunctionResponse<>(task), handler));
	}

	public LoopedInPromise(MessengerTask<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(task, handler));
	}

	public static <TResult, TNewResult> PromisedResponse<TResult, TNewResult> response(ImmediateResponse<TResult, TNewResult> task, Context context) {
		return response(task, new Handler(context.getMainLooper()));
	}

	public static <TResult, TNewResult> PromisedResponse<TResult, TNewResult> response(ImmediateResponse<TResult, TNewResult> task, Handler handler) {
		return new OneParameterExecutors.ReducingFunction<>(task, handler);
	}

	private static class Executors {
		static final class LoopedInResponse<Result> implements MessengerTask<Result>, Runnable {

			private final MessengerTask<Result> task;
			private final Handler handler;
			private Messenger<Result> resultMessenger;

			LoopedInResponse(MessengerTask<Result> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void execute(Messenger<Result> resultMessenger) {
				this.resultMessenger = resultMessenger;

				if (handler.getLooper().getThread() == Thread.currentThread())
					run();
				else
					handler.post(this);
			}

			@Override
			public void run() {
				task.execute(resultMessenger);
			}
		}
	}

	private static class OneParameterExecutors {

		static class ReducingFunction<TResult, TNewResult> implements PromisedResponse<TResult, TNewResult>, MessageTask<TNewResult> {

			private final ImmediateResponse<TResult, TNewResult> callable;
			private final Handler handler;

			private TResult result;

			ReducingFunction(ImmediateResponse<TResult, TNewResult> callable, Handler handler) {
				this.callable = callable;
				this.handler = handler;
			}

			@Override
			public TNewResult prepareMessage() throws Throwable {
				return callable.respond(result);
			}

			@Override
			public Promise<TNewResult> promiseResponse(TResult result) throws Throwable {
				this.result = result;
				return new LoopedInPromise<>(this, handler);
			}
		}
	}
}
