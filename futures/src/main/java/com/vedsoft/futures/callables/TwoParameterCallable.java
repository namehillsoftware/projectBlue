package com.vedsoft.futures.callables;

/**
 * Created by david on 12/5/15.
 */
public interface TwoParameterCallable<TFirstParameter, TSecondParameter, TResult> {
	TResult call(TFirstParameter parameterOne, TSecondParameter parameterTwo);
}
