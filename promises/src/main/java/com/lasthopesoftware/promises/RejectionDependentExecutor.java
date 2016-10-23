package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/19/16.
 */
class RejectionDependentExecutor<TResult, TNewRejectedResult> implements ThreeParameterRunnable<TResult, Exception, IPromiseResolution<TNewRejectedResult>> {
	private final TwoParameterRunnable<Exception, IPromiseResolution<TNewRejectedResult>> onRejected;

	RejectionDependentExecutor(@NotNull TwoParameterRunnable<Exception, IPromiseResolution<TNewRejectedResult>> onRejected) {
		this.onRejected = onRejected;
	}

	@Override
	public void run(TResult result, Exception exception, IPromiseResolution<TNewRejectedResult> resolution) {
		onRejected.run(exception, resolution);
	}
}
