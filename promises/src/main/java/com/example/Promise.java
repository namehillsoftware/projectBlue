package com.example;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

public class Promise<TResult> extends UnresolvedPromise<Void, TResult> {

	public Promise(TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		super((voidResult, resolve, reject) -> {
			executor.run(resolve, reject);
		});

		execute(null);
	}
}
