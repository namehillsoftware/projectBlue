package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;

import java.util.concurrent.Callable;

/**
 * Created by david on 2/12/17.
 */
class WrappedCallable<TResult> implements Runnable {
	private final Callable<TResult> callable;
	private final IRejectedPromise reject;
	private final IResolvedPromise<TResult> resolve;

	WrappedCallable(Callable<TResult> callable, IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
		this.callable = callable;
		this.reject = reject;
		this.resolve = resolve;
	}

	@Override
	public void run() {
		try {
			resolve.withResult(this.callable.call());
		} catch (Exception e) {
			reject.withError(e);
		}
	}
}
