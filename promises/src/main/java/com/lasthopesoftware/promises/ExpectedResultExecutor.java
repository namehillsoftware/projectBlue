package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class ExpectedResultExecutor<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
	private final OneParameterFunction<TResult, TNewResult> onFulfilled;

	ExpectedResultExecutor(@NotNull OneParameterFunction<TResult, TNewResult> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void runWith(TResult originalResult, IResolvedPromise<TNewResult> newResolve, IRejectedPromise newReject) {
		try {
			newResolve.withResult(onFulfilled.expectUsing(originalResult));
		} catch (Exception e) {
			newReject.withError(e);
		}
	}
}
