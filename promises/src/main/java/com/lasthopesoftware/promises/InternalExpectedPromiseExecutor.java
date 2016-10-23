package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Created by david on 10/17/16.
 */
class InternalExpectedPromiseExecutor<TResult> implements TwoParameterRunnable<IResolvedPromise<TResult>, IRejectedPromise> {
	private final Callable<TResult> executor;

	InternalExpectedPromiseExecutor(@NotNull Callable<TResult> executor) {
		this.executor = executor;
	}

	@Override
	public void run(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
		try {
			resolve.withResult(executor.call());
		} catch (Exception e) {
			reject.withError(e);
		}
	}
}
