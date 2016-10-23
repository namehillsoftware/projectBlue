package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

/**
 * Created by david on 10/18/16.
 */
class ErrorPropagatingResolveExecutor<TResult, TNewResult> implements FourParameterRunnable<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> {
	private final ThreeParameterRunnable<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

	ErrorPropagatingResolveExecutor(ThreeParameterRunnable<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void run(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
		if (exception != null) {
			reject.withError(exception);
			return;
		}

		onFulfilled.run(result, resolve, reject);
	}
}
