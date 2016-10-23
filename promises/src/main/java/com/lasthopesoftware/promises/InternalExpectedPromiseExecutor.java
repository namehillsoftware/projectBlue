package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Created by david on 10/17/16.
 */
class InternalExpectedPromiseExecutor<TResult> implements OneParameterRunnable<IPromiseResolution<TResult>> {
	private final Callable<TResult> executor;

	InternalExpectedPromiseExecutor(@NotNull Callable<TResult> executor) {
		this.executor = executor;
	}

	@Override
	public void run(IPromiseResolution<TResult> resolution) {
		try {
			resolution.fulfilled(executor.call());
		} catch (Exception e) {
			resolution.rejected(e);
		}
	}
}
