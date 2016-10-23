package com.vedsoft.futures.callables;

/**
 * Created by david on 12/5/15.
 */
public interface TwoParameterFunction<TFirstParameter, TSecondParameter, TResult> {
	TResult expectedUsing(TFirstParameter parameterOne, TSecondParameter parameterTwo);
}
