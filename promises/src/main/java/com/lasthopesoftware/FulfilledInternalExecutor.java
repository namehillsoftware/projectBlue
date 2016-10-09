package com.lasthopesoftware;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
class FulfilledInternalExecutor<TResult, TNewResult> implements ThreeParameterRunnable<TResult, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> {
	private final OneParameterCallable<TResult, TNewResult> onFulfilled;

	FulfilledInternalExecutor(OneParameterCallable<TResult, TNewResult> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void run(TResult originalResult, OneParameterRunnable<TNewResult> newResolve, OneParameterRunnable<Exception> newReject) {
		try {
			newResolve.run(onFulfilled.call(originalResult));
		} catch (Exception e) {
			newReject.run(e);
		}
	}
}
