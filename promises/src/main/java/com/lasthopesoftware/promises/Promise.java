package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

public class Promise<TResult> extends DependentPromise<Void, TResult> {

	public Promise(@NotNull TwoParameterRunnable<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		super(new InternalPromiseExecutor<>(executor));

		provide(null, null);
	}

}
