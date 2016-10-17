package com.lasthopesoftware.promises.unfulfilled;

import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnfulfilledPromise<TOriginalResult, TResult> implements IPromise<TResult> {

	private final ThreeParameterRunnable<TOriginalResult, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;
	private UnfulfilledPromise<TResult, ?> resolution;
	private UnfulfilledPromise<Exception, Void> rejection;

	private TResult fulfilledResult;
	private Exception fulfilledError;

	protected UnfulfilledPromise(@NotNull ThreeParameterRunnable<TOriginalResult, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		this.executor = executor;
	}

	protected final void fulfill(@Nullable TOriginalResult originalResult) {
		this.executor.run(originalResult, result -> {
			if (resolution != null)
				resolution.fulfill(result);

			fulfilledResult = result;
		}, error -> {
			if (rejection != null)
				rejection.fulfill(error);

			fulfilledError = error;
		});
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected) {
		final UnfulfilledPromise<TResult, TNewResult> newResolution =
				new UnfulfilledPromise<>(new FulfilledExecutor<>(onFulfilled));

		if (onRejected != null)
			error(onRejected);

		resolution = newResolution;

		if (fulfilledResult != null)
			newResolution.fulfill(fulfilledResult);

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
	public final IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected) {
		rejection = new UnfulfilledPromise<>(new RejectedExecutor(onRejected));

		if (fulfilledError != null)
			rejection.fulfill(fulfilledError);

		return rejection;
	}
}
