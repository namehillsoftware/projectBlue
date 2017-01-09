package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {

	<TNewResult> IPromise<TNewResult> thenPromise(OneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(OneParameterFunction<TResult, TNewResult> onFulfilled);

	<TNewRejectedResult> IPromise<TNewRejectedResult> error(ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected);
	<TNewRejectedResult> IPromise<TNewRejectedResult> error(OneParameterFunction<Exception, TNewRejectedResult> onRejected);

	<TNewResult> IPromise<TNewResult> thenPromise(TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled);

	<TNewRejectedResult> IPromise<TNewRejectedResult> error(FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected);
	<TNewRejectedResult> IPromise<TNewRejectedResult> error(TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected);

	void cancel();
}