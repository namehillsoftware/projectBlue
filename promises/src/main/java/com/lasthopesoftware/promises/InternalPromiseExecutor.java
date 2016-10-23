package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class InternalPromiseExecutor<TResult> implements FourParameterRunnable<Void, Exception, IResolvedPromise<TResult>, IRejectedPromise> {
	private final TwoParameterRunnable<IResolvedPromise<TResult>, IRejectedPromise> executor;

	InternalPromiseExecutor(@NotNull TwoParameterRunnable<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this.executor = executor;
	}

	@Override
	public void run(Void ignoredResult, Exception ignoredException, IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
		executor.run(resolve, reject);
	}
}
