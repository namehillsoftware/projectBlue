package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DependentPromise<TInput, TResult> implements IPromise<TResult> {

	private final ThreeParameterRunnable<TInput, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;
	private DependentPromise<TResult, ?> resolution;
	private DependentPromise<Exception, ?> rejection;

	private TResult fulfilledResult;
	private Exception fulfilledError;

	DependentPromise(@NotNull ThreeParameterRunnable<TInput, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		this.executor = executor;
	}

	final void provide(@Nullable TInput input) {
		this.executor.run(input, result -> {
			if (resolution != null)
				resolution.provide(result);

			fulfilledResult = result;
		}, error -> {
			if (rejection != null)
				rejection.provide(error);

			fulfilledError = error;
		});
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterRunnable<TResult, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> onFulfilled) {
		final DependentPromise<TResult, TNewResult> newResolution = new DependentPromise<>(onFulfilled);

		resolution = newResolution;

		if (fulfilledResult != null)
			newResolution.provide(fulfilledResult);

		return newResolution;
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull final OneParameterCallable<TResult, TNewResult> onFulfilled) {
		return then(new FulfilledExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled) {
		return then(new NullReturnRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterRunnable<Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> onRejected) {
		final DependentPromise<Exception, TNewRejectedResult> newResolution = new DependentPromise<>(onRejected);

		rejection = newResolution;

		if (fulfilledError != null)
			newResolution.provide(fulfilledError);

		return newResolution;
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterCallable<Exception, TNewRejectedResult> onRejected) {
		return error(new FulfilledExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public final IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected) {
		return error(new NullReturnRunnable<>(onRejected));
	}
}
