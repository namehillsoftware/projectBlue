package com.lasthopesoftware.promises;

import com.lasthopesoftware.promises.unfulfilled.UnfulfilledPromise;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

public class Promise<TResult> extends UnfulfilledPromise<Void, TResult> {

	public Promise(@NotNull TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		super(new InternalPromiseExecutor<>(executor));

		fulfill(null);
	}

}
