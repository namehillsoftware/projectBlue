package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 10/30/16.
 */
class InternalCancellablePromiseExecutor<TResult> implements FiveParameterAction<Void, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
	private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

	InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this.executor = executor;
	}

	@Override
	public void runWith(Void result, Exception exception, IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		executor.runWith(resolve, reject, onCancelled);
	}
}
