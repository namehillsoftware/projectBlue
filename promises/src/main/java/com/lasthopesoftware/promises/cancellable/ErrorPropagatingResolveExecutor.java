package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 10/18/16.
 */
class ErrorPropagatingResolveExecutor<TResult, TNewResult> implements FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> {
	private final ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

	ErrorPropagatingResolveExecutor(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
		if (exception != null) {
			reject.withError(exception);
			return;
		}

		onFulfilled.runWith(result, resolve, reject);
	}
}
