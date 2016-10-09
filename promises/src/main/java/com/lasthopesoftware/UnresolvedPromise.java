package com.lasthopesoftware;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
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

	final void execute(TOriginalResult originalResult) {
		this.executor.run(originalResult, result -> {
			if (resolution != null)
				resolution.execute(result);
		}, error -> {
			if (rejection != null)
				rejection.execute(error);
		});
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected) {
		final UnresolvedPromise<TResult, TNewResult> newResolution =
				new UnresolvedPromise<>(new FulfilledInternalExecutor<>(onFulfilled));

		if (onRejected != null)
			error(onRejected);

		this.resolution = newResolution;

		return newResolution;
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(final OneParameterCallable<TResult, TNewResult> onFulfilled) {
		return then(onFulfilled, null);
	}

	@Override
	public final IPromise<Void> then(OneParameterRunnable<TResult> onFulfilled) {
		return then(new FulfilledRunnableExecutor<>(onFulfilled));
	}

	@Override
	public final IPromise<Void> error(OneParameterRunnable<Exception> onRejected) {
		rejection = new UnresolvedPromise<>(new InternalErrorExecutor(onRejected));

		return rejection;
	}

}
