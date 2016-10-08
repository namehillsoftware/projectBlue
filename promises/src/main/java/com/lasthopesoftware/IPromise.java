package com.lasthopesoftware;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled, @Nullable OneParameterRunnable<Exception> onRejected);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled);
	@NotNull IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled);
	@NotNull IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected);
}