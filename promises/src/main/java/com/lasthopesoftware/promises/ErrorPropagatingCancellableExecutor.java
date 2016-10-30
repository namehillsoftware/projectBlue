package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;

/**
 * Created by david on 10/30/16.
 */
class ErrorPropagatingCancellableExecutor<TResult, TNewResult> implements FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
	private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled;

	ErrorPropagatingCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		if (exception != null) {
			reject.withError(exception);
			return;
		}

		onFulfilled.runWith(result, resolve, reject, onCancelled);
	}
}
