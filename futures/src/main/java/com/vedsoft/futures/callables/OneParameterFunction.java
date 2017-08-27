package com.vedsoft.futures.callables;

/**
 * Created by david on 11/25/15.
 */
public interface OneParameterFunction<TParameter, TResult> {
	TResult resultFrom(TParameter parameter);
}
