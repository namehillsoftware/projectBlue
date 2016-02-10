package com.vedsoft.futures.callables;

/**
 * Created by david on 11/25/15.
 */
public interface OneParameterCallable<TParameter, TResult> {
	TResult call(TParameter parameter);
}
