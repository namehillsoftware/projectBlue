package com.lasthopesoftware.promises;

import com.lasthopesoftware.promises.cancellable.ICancellablePromise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DependentPromise<TInput, TResult> implements IPromise<TResult> {

	private final DependentCancellablePromise<TInput, TResult> internalCancellablePromise;

	DependentPromise(@NotNull FourParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this(new DependentCancellablePromise<>((input, exception, resolve, reject, onCancelled) -> {
			onCancelled.runWith(NoOpRunnable.getInstance());
			executor.runWith(input, exception, resolve, reject);
		}));
	}

	DependentPromise(@NotNull DependentCancellablePromise<TInput, TResult> internalCancellablePromise) {
		this.internalCancellablePromise = internalCancellablePromise;
	}

	final void provide(@Nullable TInput input, @Nullable Exception exception) {
		internalCancellablePromise.provide(input, exception);
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return internalCancellablePromise.mutateCancellablePromise(
			internalCancellablePromise.then((FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>>)(result, exception, resolve, reject, onCancelled) -> {
			onFulfilled.runWith(result, exception, resolve, reject);
		}));
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

	@NotNull
	@Override
	public ICancellablePromise<TResult> cancellable() {
		return internalCancellablePromise;
	}
}
