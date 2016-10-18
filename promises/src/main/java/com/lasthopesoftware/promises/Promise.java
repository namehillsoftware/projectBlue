package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

public class Promise<TResult> extends DependentPromise<Void, TResult> {

	public Promise(@NotNull TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		super(new InternalPromiseExecutor<>(executor));

		provide(null);
	}

}
