package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class InternalPromiseExecutor<TResult> implements ThreeParameterRunnable<Void, Exception, IPromiseResolution<TResult>> {
	private final OneParameterRunnable<IPromiseResolution<TResult>> executor;

	InternalPromiseExecutor(@NotNull OneParameterRunnable<IPromiseResolution<TResult>> executor) {
		this.executor = executor;
	}

	@Override
	public void run(Void ignoredResult, Exception ignoredException, IPromiseResolution<TResult> resolution) {
		executor.run(resolution);
	}
}
