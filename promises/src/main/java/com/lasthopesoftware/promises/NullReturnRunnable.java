package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class NullReturnRunnable<TResult> implements OneParameterFunction<TResult, Void> {
	private final OneParameterAction<TResult> resolve;

	NullReturnRunnable(@NotNull OneParameterAction<TResult> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void expectedUsing(TResult result) {
		resolve.runWith(result);
		return null;
	}
}
