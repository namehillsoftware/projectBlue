package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/25/16.
 */

public interface ICancellablePromise<TResult> {
	void cancel();

	@NotNull
	<TNewResult> ICancellablePromise<TNewResult> then(@NotNull FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled);
	@NotNull <TNewResult> ICancellablePromise<TNewResult> then(@NotNull FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled);
	@NotNull <TNewResult> ICancellablePromise<TNewResult> then(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled);
	@NotNull ICancellablePromise<Void> then(@NotNull TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled);

	@NotNull <TNewRejectedResult> ICancellablePromise<TNewRejectedResult> error(@NotNull FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected);
	@NotNull <TNewRejectedResult> ICancellablePromise<TNewRejectedResult> error(@NotNull TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected);
	@NotNull ICancellablePromise<Void> error(@NotNull TwoParameterAction<Exception, OneParameterAction<Runnable>> onRejected);
}
