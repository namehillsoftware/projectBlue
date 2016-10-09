package com.lasthopesoftware;

import com.sun.istack.internal.Nullable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
class InternalResolution<TResult> implements OneParameterRunnable<TResult> {

	private final UnresolvedPromise<TResult, ?> fulfilledPromise;

	InternalResolution(@Nullable UnresolvedPromise<TResult, ?> fulfilledPromise) {
		this.fulfilledPromise = fulfilledPromise;
	}

	@Override
	public void run(TResult result) {
		if (fulfilledPromise != null)
			fulfilledPromise.execute(result);
	}
}
