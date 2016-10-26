package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 10/19/16.
 */
class RejectionDependentExecutor<TResult, TNewRejectedResult> implements FourParameterAction<TResult, Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> {
	private final ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected;

	RejectionDependentExecutor(ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		this.onRejected = onRejected;
	}

	@Override
	public void runWith(TResult result, Exception exception, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject) {
		onRejected.runWith(exception, resolve, reject);
	}
}
