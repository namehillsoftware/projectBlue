package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class NullReturnRunnable<TResult> implements OneParameterCallable<TResult, Void> {
	private final OneParameterRunnable<TResult> resolve;

	NullReturnRunnable(@NotNull OneParameterRunnable<TResult> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void call(TResult result) {
		resolve.run(result);
		return null;
	}
}
