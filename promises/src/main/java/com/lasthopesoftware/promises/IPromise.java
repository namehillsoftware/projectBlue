package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {

	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterRunnable<TResult, Exception, IPromiseResolution<TNewResult>> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull TwoParameterRunnable<TResult, IPromiseResolution<TNewResult>> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterCallable<TResult, TNewResult> onFulfilled);
	@NotNull IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled);

	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull TwoParameterRunnable<Exception, IPromiseResolution<TNewRejectedResult>> onRejected);
	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterCallable<Exception, TNewRejectedResult> onRejected);
	@NotNull IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected);
}