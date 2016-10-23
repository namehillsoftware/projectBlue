package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class ExpectedResultExecutor<TResult, TNewResult> implements ThreeParameterRunnable<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
	private final OneParameterCallable<TResult, TNewResult> onFulfilled;

	ExpectedResultExecutor(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void run(TResult originalResult, IResolvedPromise<TNewResult> newResolve, IRejectedPromise newReject) {
		try {
			newResolve.withResult(onFulfilled.call(originalResult));
		} catch (Exception e) {
			newReject.withError(e);
		}
	}
}
