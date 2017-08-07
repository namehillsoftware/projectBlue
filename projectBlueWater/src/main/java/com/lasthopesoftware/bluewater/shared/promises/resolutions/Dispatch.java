package com.lasthopesoftware.bluewater.shared.promises.resolutions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.messenger.promises.Promise;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

public class Dispatch {
	public static <TResult, TNewResult> CarelessOneParameterFunction<TResult, Promise<TNewResult>> toContext(CarelessOneParameterFunction<TResult, TNewResult> task, Context context) {
		return toHandler(task, new Handler(context.getMainLooper()));
	}

	public static <TResult, TNewResult> CarelessOneParameterFunction<TResult, Promise<TNewResult>> toHandler(CarelessOneParameterFunction<TResult, TNewResult> task, Handler handler) {
		return new OneParameterExecutors.ReducingFunction<>(task, handler);
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
				return new LoopedInPromise<>(this, handler);
			}
		}
	}
}
