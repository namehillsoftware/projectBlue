package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class ExpectedResultExecutor<TResult, TNewResult> implements TwoParameterRunnable<TResult, IPromiseResolution<TNewResult>> {
	private final OneParameterCallable<TResult, TNewResult> onFulfilled;

	ExpectedResultExecutor(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void run(TResult originalResult, IPromiseResolution<TNewResult> resolution) {
		try {
			resolution.fulfilled(onFulfilled.call(originalResult));
		} catch (Exception e) {
			resolution.rejected(e);
		}
	}
}
