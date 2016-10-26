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
public class CancellablePromise<TInput, TResult> implements ICancellablePromise<TResult> {

	private final FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

	private CancellablePromise<TResult, ?> resolution;

	private Runnable onCancellation;
	private TResult fulfilledResult;
	private Exception fulfilledError;
	private boolean isResolved;
	private final Object resolutionSync = new Object();

	CancellablePromise(@NotNull FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this.executor = executor;
	}

	final void provide(@Nullable TInput input, @Nullable Exception exception, @Nullable Runnable cancellation) {
		this.executor.runWith(input, exception, result -> {
			fulfilledResult = result;

			resolve(result, null);
		}, error -> {
			fulfilledError = error;

			resolve(null, error);
		}, onCancellation -> this.onCancellation = () -> {
			onCancellation.run();

			if (cancellation != null)
				cancellation.run();
		});
	}

	private void resolve(TResult result, Exception error) {
		isResolved = true;

		synchronized (resolutionSync) {
			if (resolution != null)
				resolution.provide(result, error, this::cancel);
		}
	}

	@Override
	public void cancel() {
		onCancellation.run();
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return null;
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return null;
	}

	@NotNull
	@Override
	public <TNewResult> ICancellablePromise<TNewResult> then(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return null;
	}

	@NotNull
	@Override
	public ICancellablePromise<Void> then(@NotNull TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled) {
		return null;
	}

	@NotNull
	@Override
	public <TNewRejectedResult> ICancellablePromise<TNewRejectedResult> error(@NotNull FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return null;
	}

	@NotNull
	@Override
	public <TNewRejectedResult> ICancellablePromise<TNewRejectedResult> error(@NotNull TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return null;
	}

	@NotNull
	@Override
	public ICancellablePromise<Void> error(@NotNull TwoParameterAction<Exception, OneParameterAction<Runnable>> onRejected) {
		return null;
	}
}
