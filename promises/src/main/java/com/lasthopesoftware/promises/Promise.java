package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterRunnable;

import org.jetbrains.annotations.NotNull;

public class Promise<TResult> extends DependentPromise<Void, TResult> {

	public Promise(@NotNull OneParameterRunnable<IPromiseResolution<TResult>> executor) {
		super(new InternalPromiseExecutor<>(executor));

		provide(null, null);
	}

}
