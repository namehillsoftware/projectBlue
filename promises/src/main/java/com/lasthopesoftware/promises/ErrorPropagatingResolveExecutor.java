package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/18/16.
 */
class ErrorPropagatingResolveExecutor<TResult, TNewResult> implements ThreeParameterRunnable<TResult, Exception, IPromiseResolution<TNewResult>> {
	private final TwoParameterRunnable<TResult, IPromiseResolution<TNewResult>> onFulfilled;

	ErrorPropagatingResolveExecutor(@NotNull TwoParameterRunnable<TResult, IPromiseResolution<TNewResult>> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void run(TResult result, Exception exception, IPromiseResolution<TNewResult> resolution) {
		if (exception != null) {
			resolution.rejected(exception);
			return;
		}

		onFulfilled.run(result, resolution);
	}
}
