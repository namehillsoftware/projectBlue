package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

/**
 * Created by david on 10/19/16.
 */
class RejectionDependentExecutor<TResult, TNewRejectedResult> implements FourParameterRunnable<TResult, Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> {
	private final ThreeParameterRunnable<Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> onRejected;

	RejectionDependentExecutor(ThreeParameterRunnable<Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> onRejected) {
		this.onRejected = onRejected;
	}

	@Override
	public void run(TResult result, Exception exception, OneParameterRunnable<TNewRejectedResult> resolve, OneParameterRunnable<Exception> reject) {
		onRejected.run(exception, resolve, reject);
	}
}
