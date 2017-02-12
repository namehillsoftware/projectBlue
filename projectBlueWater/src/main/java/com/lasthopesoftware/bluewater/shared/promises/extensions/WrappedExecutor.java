package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 2/12/17.
 */
class WrappedExecutor<TResult> implements Runnable {
	private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task;
	private final IRejectedPromise reject;
	private final IResolvedPromise<TResult> resolve;

	WrappedExecutor(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task, IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
		this.task = task;
		this.reject = reject;
		this.resolve = resolve;
	}

	@Override
	public void run() {
		this.task.runWith(resolve, reject);
	}
}
