package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

/**
 * Created by david on 10/18/16.
 */
class ErrorPropagatingResolveExecutor<TResult, TNewResult> implements FourParameterRunnable<TResult, Exception, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> {
	private final ThreeParameterRunnable<TResult, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> onFulfilled;

	ErrorPropagatingResolveExecutor(ThreeParameterRunnable<TResult, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	public void run(TResult result, Exception exception, OneParameterRunnable<TNewResult> resolve, OneParameterRunnable<Exception> reject) {
		if (exception != null) {
			reject.run(exception);
			return;
		}

		onFulfilled.run(result, resolve, reject);
	}
}
