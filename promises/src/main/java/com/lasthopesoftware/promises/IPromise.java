package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {

	@NotNull <TNewResult> IPromise<TNewResult> thenPromise(@NotNull OneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull OneParameterFunction<TResult, TNewResult> onFulfilled);
	@NotNull IPromise<Void> then(@NotNull OneParameterAction<TResult> onFulfilled);

	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected);
	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterFunction<Exception, TNewRejectedResult> onRejected);
	@NotNull IPromise<Void> error(@NotNull OneParameterAction<Exception> onRejected);

	@NotNull <TNewResult> IPromise<TNewResult> thenPromise(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled);
	@NotNull <TNewResult> IPromise<TNewResult> then(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled);
	@NotNull IPromise<Void> then(@NotNull TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled);

	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected);
	@NotNull <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected);
	@NotNull IPromise<Void> error(@NotNull TwoParameterAction<Exception, OneParameterAction<Runnable>> onRejected);

	void cancel();
}