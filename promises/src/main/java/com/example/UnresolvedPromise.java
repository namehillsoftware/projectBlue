package com.example;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

class UnresolvedPromise<TOriginalResult, TResult> implements IPromise<TResult> {

	private final ThreeParameterRunnable<TOriginalResult, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;
	private UnresolvedPromise<TResult, ?> resolution;
	private UnresolvedPromise<Exception, Void> rejection;

	UnresolvedPromise(ThreeParameterRunnable<TOriginalResult, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		this.executor = executor;
	}

	void execute(TOriginalResult originalResult) {
		this.executor.run(originalResult, (result) -> {
			if (resolution != null)
				resolution.execute(result);
		}, (error) -> {
			if (rejection != null)
				rejection.execute(error);
		});
	}

	@Override
	public <TNewResult> IPromise<TNewResult> then(final OneParameterCallable<TResult, TNewResult> onFulfilled) {
		final UnresolvedPromise<TResult, TNewResult> newResolution = new UnresolvedPromise<>((originalResult, newResolve, newReject) -> {
			try {
				newResolve.run(onFulfilled.call(originalResult));
			} catch (Exception e) {
				newReject.run(e);
			}
		});

		this.resolution = newResolution;

		return newResolution;
	}

	@Override
	public IPromise<Void> then(OneParameterRunnable<TResult> resolve) {
		return then(result -> {
			resolve.run(result);
			return null;
		});
	}

	@Override
	public IPromise<Void> error(OneParameterRunnable<Exception> onRejected) {
		rejection = new UnresolvedPromise<>((exception, newResolve, newReject) -> {
			try {
				onRejected.run(exception);
				newResolve.run(null);
			} catch (Exception e) {
				newReject.run(e);
			}
		});

		return rejection;
	}
}
