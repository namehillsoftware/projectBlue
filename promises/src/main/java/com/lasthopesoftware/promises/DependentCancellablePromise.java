package com.lasthopesoftware.promises;

import com.lasthopesoftware.promises.cancellable.Cancellation;
import com.lasthopesoftware.promises.cancellable.ICancellablePromise;
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

	private volatile boolean isResolved;
	private volatile boolean isResolutionHandled;

	private TResult fulfilledResult;
	private Exception fulfilledError;

	private final Cancellation cancellation = new Cancellation();

	DependentCancellablePromise(@NotNull FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this.executor = executor;
	}

	public void provide(@Nullable TInput input, @Nullable Exception exception) {
		this.executor.runWith(
			input,
			exception,
			result -> resolve(result, null),
			error -> resolve(null, error),
			this.cancellation::onCancelled);
	}

	public void cancel() {
		this.cancellation.cancel();
	}

	private void resolve(TResult result, Exception error) {
		fulfilledResult = result;
		fulfilledError = error;

		isResolved = true;

		DependentCancellablePromise<TResult, ?> localResolution = resolution;

		if (localResolution != null)
			handleResolution(localResolution, result, error);
	}

	private void handleResolution(@NotNull DependentCancellablePromise<TResult, ?> resolution, @Nullable TResult result, @Nullable Exception error) {
		if (isResolutionHandled) return;

		resolution.provide(result, error);
		isResolutionHandled = true;
	}

	<TNewResult> DependentPromise<TResult, TNewResult> mutateCancellablePromise(DependentCancellablePromise<TResult, TNewResult> internalCancellablePromise) {
		resolution = internalCancellablePromise;

		if (isResolved)
			handleResolution(internalCancellablePromise, fulfilledResult, fulfilledError);

		return new DependentPromise<>(internalCancellablePromise);
	}

	@NotNull
	@Override
	public <TNewResult> DependentCancellablePromise<TResult, TNewResult> then(@NotNull FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		final DependentCancellablePromise<TResult, TNewResult> newResolution = new DependentCancellablePromise<>(onFulfilled);

		resolution = newResolution;

		if (isResolved)
			handleResolution(newResolution, fulfilledResult, fulfilledError);

		return newResolution;
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return then((result, exception, resolve, reject, onCancelled) -> {
			if (exception != null) {
				reject.withError(exception);
				return;
			}

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
