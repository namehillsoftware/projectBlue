package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class InternalPromiseExecutor<TResult> implements FourParameterAction<Void, Exception, IResolvedPromise<TResult>, IRejectedPromise> {
	private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor;

	InternalPromiseExecutor(@NotNull TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this.executor = executor;
	}

	@Override
	public void runWith(Void ignoredResult, Exception ignoredException, IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
		executor.runWith(resolve, reject);
	}
}
