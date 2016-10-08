package com.lasthopesoftware;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

abstract class UnresolvedPromise<TOriginalResult, TResult> implements IPromise<TResult> {

	private UnresolvedPromise<TResult, ?> resolution;
	private UnresolvedPromise<Exception, Void> rejection;


	protected abstract void execute(TOriginalResult result);

	protected final void resolve(TResult result) {
		if (resolution != null)
			resolution.execute(result);
	}

	protected final void reject(Exception error) {
		if (rejection != null)
			rejection.execute(error);
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected) {
		final UnresolvedPromise<TResult, TNewResult> newResolution = new UnresolvedPromise<TResult, TNewResult>() {
			@Override
			protected void execute(TResult result) {
				try {
					resolve(onFulfilled.call(result));
				} catch (Exception e) {
					reject(e);
				}
			}
		};

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
	public IPromise<Void> then(OneParameterRunnable<TResult> resolve) {
		return then(result -> {
			resolve.run(result);
			return null;
		});
	}

	@Override
	public IPromise<Void> error(OneParameterRunnable<Exception> onRejected) {
		rejection = new UnresolvedPromise<Exception, Void>() {
			@Override
			protected void execute(Exception exception) {
				try {
					onRejected.run(exception);
					resolve(null);
				} catch (Exception e) {
					reject(e);
				}
			}
		};

		return rejection;
	}
}
