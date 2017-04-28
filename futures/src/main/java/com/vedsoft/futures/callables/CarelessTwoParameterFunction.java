package com.vedsoft.futures.callables;

/**
 * Created by david on 12/5/15.
 */
public interface CarelessTwoParameterFunction<TFirstParameter, TSecondParameter, TResult> {
	TResult resultFrom(TFirstParameter parameterOne, TSecondParameter parameterTwo) throws Throwable;
}
