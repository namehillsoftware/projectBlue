package com.lasthopesoftware;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
class FulfilledRunnableExecutor<TResult> implements OneParameterCallable<TResult, Void> {
	private final OneParameterRunnable<TResult> resolve;

	FulfilledRunnableExecutor(OneParameterRunnable<TResult> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void call(TResult result) {
		resolve.run(result);
		return null;
	}
}
