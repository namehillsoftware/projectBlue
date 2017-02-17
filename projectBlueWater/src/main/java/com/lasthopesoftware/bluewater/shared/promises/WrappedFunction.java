package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.CarelessFunction;

/**
 * Created by david on 2/12/17.
 */
public class WrappedFunction<TResult> implements Runnable {
	private final CarelessFunction<TResult> callable;
	private final IRejectedPromise reject;
	private final IResolvedPromise<TResult> resolve;

	public WrappedFunction(CarelessFunction<TResult> callable, IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
		this.callable = callable;
		this.reject = reject;
		this.resolve = resolve;
	}

	@Override
	public void run() {
		try {
			resolve.withResult(this.callable.result());
		} catch (Exception e) {
			reject.withError(e);
		}
	}
}
