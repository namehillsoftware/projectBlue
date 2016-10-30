package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class InternalPromiseExecutor<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
	private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor;

	InternalPromiseExecutor(@NotNull TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this.executor = executor;
	}

	@Override
	public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		onCancelled.runWith(NoOpRunnable.getInstance());
		executor.runWith(resolve, reject);
	}
}
