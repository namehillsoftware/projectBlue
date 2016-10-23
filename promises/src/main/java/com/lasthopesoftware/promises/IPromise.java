package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {

	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterFunction<TResult, TNewResult> onFulfilled);
	@NotNull IPromise<Void> then(@NotNull OneParameterAction<TResult> onFulfilled);

	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected);
	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterFunction<Exception, TNewRejectedResult> onRejected);
	@NotNull IPromise<Void> error(@NotNull OneParameterAction<Exception> onRejected);
}