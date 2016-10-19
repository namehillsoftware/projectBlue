package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class InternalPromiseExecutor<TResult> implements FourParameterRunnable<Void, Exception, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> {
	private final TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;

	InternalPromiseExecutor(@NotNull TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		this.executor = executor;
	}

	@Override
	public void run(Void voidResult, Exception ignoredException, OneParameterRunnable<TResult> resolve, OneParameterRunnable<Exception> reject) {
		executor.run(resolve, reject);
	}
}
