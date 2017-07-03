package com.lasthopesoftware.bluewater.shared.promises.resolutions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.shared.promises.extensions.DispatchedPromise;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promise.Promise;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

public class Dispatch {
	public static <TResult, TNewResult> CarelessOneParameterFunction<TResult, Promise<TNewResult>> toContext(CarelessOneParameterFunction<TResult, TNewResult> task, Context context) {
		return toHandler(task, new Handler(context.getMainLooper()));
	}

	public static <TResult, TNewResult> CarelessOneParameterFunction<TResult, Promise<TNewResult>> toHandler(CarelessOneParameterFunction<TResult, TNewResult> task, Handler handler) {
		return new OneParameterExecutors.ReducingFunction<>(task, handler);
	}

	public static <Result, NewResult> CarelessOneParameterFunction<Result, Promise<NewResult>> toHandler(TwoParameterAction<Result, Messenger<NewResult>> task, Handler handler) {
		return new OneParameterExecutors.ReducingAction<>(task, handler);
	}

	private static class OneParameterExecutors {

		static class ReducingFunction<TResult, TNewResult> implements CarelessOneParameterFunction<TResult, Promise<TNewResult>>, CarelessFunction<TNewResult> {

			private final CarelessOneParameterFunction<TResult, TNewResult> callable;
			private final Handler handler;

			private TResult result;

			ReducingFunction(CarelessOneParameterFunction<TResult, TNewResult> callable, Handler handler) {
				this.callable = callable;
				this.handler = handler;
			}

			@Override
			public TNewResult result() throws Throwable {
				return callable.resultFrom(result);
			}

			@Override
			public Promise<TNewResult> resultFrom(TResult result) throws Throwable {
				this.result = result;
				return new DispatchedPromise<>(this, handler);
			}
		}

		static class ReducingAction<Result, NewResult> implements CarelessOneParameterFunction<Result, Promise<NewResult>>, OneParameterAction<Messenger<NewResult>> {

			private final TwoParameterAction<Result, Messenger<NewResult>> action;
			private final Handler handler;
			private Result result;

			ReducingAction(TwoParameterAction<Result, Messenger<NewResult>> action, Handler handler) {
				this.action = action;
				this.handler = handler;
			}

			@Override
			public void runWith(Messenger<NewResult> newResultMessenger) {
				action.runWith(result, newResultMessenger);
			}

			@Override
			public Promise<NewResult> resultFrom(Result result) throws Throwable {
				this.result = result;
				return new DispatchedPromise<>(this, handler);
			}
		}
	}
}
