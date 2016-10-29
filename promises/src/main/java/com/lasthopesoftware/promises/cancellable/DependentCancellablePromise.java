package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by david on 10/25/16.
 */
class DependentCancellablePromise<TInput, TResult> implements ICancellablePromise<TResult> {

	private final FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

	private DependentCancellablePromise<TResult, ?> resolution;

	private TResult fulfilledResult;
	private Exception fulfilledError;
	private boolean isResolved;
	private boolean isCancelled;
	private final Object resolutionSync = new Object();

	private final Cancellation cancellation;

	DependentCancellablePromise(@NotNull FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this(executor, new Cancellation());
	}

	private DependentCancellablePromise(@NotNull FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor, @NotNull Cancellation cancellation) {
		this.executor = executor;
		this.cancellation = cancellation;
	}

	final void provide(@Nullable TInput input, @Nullable Exception exception) {
		this.executor.runWith(input, exception, result -> {
			fulfilledResult = result;

			resolve(result, null);
		}, error -> {
			fulfilledError = error;

			resolve(null, error);
		}, this.cancellation::onCancelled);
	}

	private void resolve(TResult result, Exception error) {
		isResolved = true;

		synchronized (resolutionSync) {
			if (resolution != null)
				resolution.provide(result, error);
		}
	}

	@Override
	public void cancel() {
		isCancelled = true;
		this.cancellation.cancel();
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		final DependentCancellablePromise<TResult, TNewResult> newResolution = new DependentCancellablePromise<>(onFulfilled, this.cancellation);

		synchronized (resolutionSync) {
			resolution = newResolution;
		}

		if (isResolved || isCancelled)
			newResolution.provide(fulfilledResult, fulfilledError);

		return newResolution;
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return then((result, exception, resolve, reject, onCancelled) -> {
			onFulfilled.runWith(result, resolve, reject, onCancelled);
		});
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return then((result, resolve, reject, onCancelled) -> {
			try {
				resolve.withResult(onFulfilled.expectedUsing(result, onCancelled));
			} catch (Exception e) {
				reject.withError(e);
			}
		});
	}

	@NotNull
	@Override
	public ICancellablePromise<Void> then(@NotNull TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled) {
		return then((result, onCancelled) -> {
			onFulfilled.runWith(result,  onCancelled);
			return null;
		});
	}

	@NotNull
	@Override
	public <TNewRejectedResult> ICancellablePromise<TNewRejectedResult> error(@NotNull FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return then((result, error, resolve, reject, onCancelled) -> {
			onRejected.runWith(error, resolve, reject, onCancelled);
		});
	}

	@NotNull
	@Override
	public <TNewRejectedResult> ICancellablePromise<TNewRejectedResult> error(@NotNull TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return error((error, resolve, reject, onCancelled) -> {
			try {
				resolve.withResult(onRejected.expectedUsing(error, onCancelled));
			} catch (Exception e) {
				reject.withError(e);
			}
		});
	}

	@NotNull
	@Override
	public ICancellablePromise<Void> error(@NotNull TwoParameterAction<Exception, OneParameterAction<Runnable>> onRejected) {
		return error((error, onCancelled) -> {
			onRejected.runWith(error, onCancelled);
			return null;
		});
	}
}
