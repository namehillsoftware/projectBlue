package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Created by david on 10/17/16.
 */
class InternalExpectedPromiseExecutor<TResult> implements TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> {
	private final Callable<TResult> executor;

	InternalExpectedPromiseExecutor(@NotNull Callable<TResult> executor) {
		this.executor = executor;
	}

	@Override
	public void run(OneParameterRunnable<TResult> resolve, OneParameterRunnable<Exception> reject) {
		try {
			resolve.run(executor.call());
		} catch (Exception e) {
			reject.run(e);
		}
	}
}
