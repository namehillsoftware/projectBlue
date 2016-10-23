package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DependentPromise<TInput, TResult> implements IPromise<TResult> {

	private final FourParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise> executor;

	private DependentPromise<TResult, ?> resolution;
	private final Object resolutionSync = new Object();
	private volatile boolean isResolved;

	private TResult fulfilledResult;
	private Exception fulfilledError;

	DependentPromise(@NotNull FourParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this.executor = executor;
	}

	final void provide(@Nullable TInput input, @Nullable Exception exception) {
		this.executor.runWith(input, exception, result -> {
			fulfilledResult = result;

			resolve(result, null);
		}, error -> {
			fulfilledError = error;

			resolve(null, error);
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
	public <TNewResult> IPromise<TNewResult> then(@NotNull FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
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
	public <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return then(new ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull final OneParameterFunction<TResult, TNewResult> onFulfilled) {
		return then(new ExpectedResultExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final IPromise<Void> then(@NotNull OneParameterAction<TResult> onFulfilled) {
		return then(new NullReturnRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		return then(new RejectionDependentExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterFunction<Exception, TNewRejectedResult> onRejected) {
		return error(new ExpectedResultExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public final IPromise<Void> error(@NotNull OneParameterAction<Exception> onRejected) {
		return error(new NullReturnRunnable<>(onRejected));
	}

}
