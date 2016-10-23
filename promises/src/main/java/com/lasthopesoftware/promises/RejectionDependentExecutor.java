package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

/**
 * Created by david on 10/19/16.
 */
class RejectionDependentExecutor<TResult, TNewRejectedResult> implements FourParameterRunnable<TResult, Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> {
	private final ThreeParameterRunnable<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected;

	RejectionDependentExecutor(ThreeParameterRunnable<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		this.onRejected = onRejected;
	}

	@Override
	public void run(TResult result, Exception exception, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject) {
		onRejected.run(exception, resolve, reject);
	}
}
