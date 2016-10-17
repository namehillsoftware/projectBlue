package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled);
	@NotNull IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled);
	@NotNull IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected);
}