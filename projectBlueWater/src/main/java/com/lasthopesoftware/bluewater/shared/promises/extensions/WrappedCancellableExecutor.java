package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 2/12/17.
 */
public class WrappedCancellableExecutor<TResult> implements Runnable {
	private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task;
	private final OneParameterAction<Runnable> onCancelled;
	private final IRejectedPromise reject;
	private final IResolvedPromise<TResult> resolve;

	WrappedCancellableExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task, IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		this.task = task;
		this.onCancelled = onCancelled;
		this.reject = reject;
		this.resolve = resolve;
	}

	@Override
	public void run() {
		this.task.runWith(resolve, reject, onCancelled);
	}
}
