package com.lasthopesoftware.promises;

import com.lasthopesoftware.promises.unfulfilled.UnfulfilledPromise;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

public class Promise<TResult> extends UnfulfilledPromise<Void, TResult> {

	public Promise(TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		super(new InternalPromiseExecutor<>(executor));

		fulfill(null);
	}

}
