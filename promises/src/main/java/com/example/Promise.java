package com.example;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

public class Promise<TResult> {

	private OneParameterRunnable<TResult> resolveWithoutContinuing;
	private OneParameterCallable<TResult, Promise<?>> resolveWithAnotherPromise;

	public Promise(TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		executor.run(result -> {
			if (resolveWithoutContinuing != null) {
				resolveWithoutContinuing.run(result);
				return;
			}

			if (this.resolveWithAnotherPromise != null)
		}, exception -> {

		});
	}

	public Promise<TResult> then(OneParameterRunnable<TResult> resolveWithoutContinuing) {
		this.resolveWithoutContinuing = resolveWithoutContinuing;
		return this;
	}

	public <TNewResult> Promise<TNewResult> then(OneParameterCallable<TResult, TNewResult> resolveWithAnotherPromise) {
		if (resolveWithoutContinuing != null)
			resolveWithoutContinuing = null;

		this.resolveWithAnotherPromise = resolveWithAnotherPromise;

		return resolveWithAnotherPromise.call(result);
	}
}
