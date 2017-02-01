package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {

	<TNewResult> IPromise<TNewResult> thenPromise(CarelessOneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(CarelessOneParameterFunction<TResult, TNewResult> onFulfilled);

	<TNewRejectedResult> IPromise<TNewRejectedResult> error(ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected);
	<TNewRejectedResult> IPromise<TNewRejectedResult> error(CarelessOneParameterFunction<Exception, TNewRejectedResult> onRejected);

	<TNewResult> IPromise<TNewResult> thenPromise(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled);
	<TNewResult> IPromise<TNewResult> then(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled);

	<TNewRejectedResult> IPromise<TNewRejectedResult> error(FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected);
	<TNewRejectedResult> IPromise<TNewRejectedResult> error(CarelessTwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected);

	void cancel();
}