package com.example;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 10/8/16.
 */

public interface IPromise<TResult> {
	<TNewResult> IPromise<TNewResult> then(OneParameterCallable<TResult, TNewResult> resolve);
	IPromise<Void> then(OneParameterRunnable<TResult> resolve);
	IPromise<Void> error(OneParameterRunnable<Exception> error);
}