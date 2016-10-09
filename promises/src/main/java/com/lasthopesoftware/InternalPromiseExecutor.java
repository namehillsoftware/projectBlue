package com.lasthopesoftware;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
class InternalPromiseExecutor<TResult> implements ThreeParameterRunnable<Void, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> {
	private final TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;

	InternalPromiseExecutor(TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		this.executor = executor;
	}

	@Override
	public void run(Void voidResult, OneParameterRunnable<TResult> resolve, OneParameterRunnable<Exception> reject) {
		executor.run(resolve, reject);
	}
}
