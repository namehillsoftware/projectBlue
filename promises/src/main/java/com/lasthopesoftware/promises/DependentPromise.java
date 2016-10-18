package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DependentPromise<TInput, TResult> implements IPromise<TResult> {

	private final ThreeParameterRunnable<TInput, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;
	private DependentPromise<TResult, ?> resolution;
	private DependentPromise<Exception, Void> rejection;

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
	public <TNewResult, TNewRejectedResult> IPromise<TNewResult> then(@NotNull ThreeParameterRunnable<TResult, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> onFulfilled, @NotNull ThreeParameterRunnable<Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> onRejected) {
		final IPromise<TNewResult> newResolution = then(onFulfilled);

		error(onRejected);

		return newResolution;
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected) {
		return then(new FulfilledExecutor<>(onFulfilled), new RejectedExecutor(onRejected));
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

	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull final OneParameterCallable<TResult, TNewResult> onFulfilled) {
		return then(onFulfilled, null);
	}

	@Override
	public final IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled) {
		return then(new NullReturnRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterRunnable<Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> onRejected) {
		return null;
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterCallable<Exception, TNewRejectedResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected) {
		return null;
	}

	@NotNull
	@Override
	public final IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected) {
		rejection = new DependentPromise<>(new RejectedExecutor(onRejected));

		if (fulfilledError != null)
			rejection.provide(fulfilledError);

		return rejection;
	}
}
