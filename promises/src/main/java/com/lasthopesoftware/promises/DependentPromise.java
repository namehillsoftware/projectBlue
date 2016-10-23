package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DependentPromise<TInput, TResult> implements IPromise<TResult> {

	private final ThreeParameterRunnable<TInput, Exception, IPromiseResolution<TResult>> executor;

	private DependentPromise<TResult, ?> resolution;
	private final Object resolutionSync = new Object();
	private volatile boolean isResolved;

	private TResult fulfilledResult;
	private Exception fulfilledError;

	DependentPromise(@NotNull ThreeParameterRunnable<TInput, Exception, IPromiseResolution<TResult>> executor) {
		this.executor = executor;
	}

	final void provide(@Nullable TInput input, @Nullable Exception exception) {
		this.executor.run(input, exception, new IPromiseResolution<TResult>() {
			@Override
			public void fulfilled(TResult result) {
				fulfilledResult = result;

				resolve(result, null);
			}

			@Override
			public void rejected(Exception error) {
				fulfilledError = error;

				resolve(null, error);
			}
		});
	}

	private void resolve(TResult result, Exception error) {
		isResolved = true;

		synchronized (resolutionSync) {
			if (resolution != null)
				resolution.provide(result, error);
		}
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterRunnable<TResult, Exception, IPromiseResolution<TNewResult>> onFulfilled) {
		final DependentPromise<TResult, TNewResult> newResolution = new DependentPromise<>(onFulfilled);

		synchronized (resolutionSync) {
			resolution = newResolution;
		}

		if (isResolved)
			newResolution.provide(fulfilledResult, fulfilledError);

		return newResolution;
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull TwoParameterRunnable<TResult, IPromiseResolution<TNewResult>> onFulfilled) {
		return then(new ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull final OneParameterCallable<TResult, TNewResult> onFulfilled) {
		return then(new ExpectedResultExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled) {
		return then(new NullReturnRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull TwoParameterRunnable<Exception, IPromiseResolution<TNewRejectedResult>> onRejected) {
		return then(new RejectionDependentExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterCallable<Exception, TNewRejectedResult> onRejected) {
		return error(new ExpectedResultExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public final IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected) {
		return error(new NullReturnRunnable<>(onRejected));
	}

}
