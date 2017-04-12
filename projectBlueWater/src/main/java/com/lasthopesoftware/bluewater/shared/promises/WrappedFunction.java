package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.CarelessFunction;

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
			resolve.sendResolution(this.callable.result());
		} catch (Exception e) {
			reject.sendRejection(e);
		}
	}
}
