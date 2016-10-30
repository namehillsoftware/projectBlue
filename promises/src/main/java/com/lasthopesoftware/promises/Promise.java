package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;

public class Promise<TResult> extends DependentCancellablePromise<Void, TResult> {

	public Promise(@NotNull ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		super(new InternalCancellablePromiseExecutor<>(executor));

		provide(null, null);
	}

	public Promise(@NotNull TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this(new InternalPromiseExecutor<>(executor));
	}

}
