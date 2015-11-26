package com.lasthopesoftware.callables;

/**
 * Created by david on 11/25/15.
 */
public interface IOneParameterCallable<TParameter, TResult> {
	TResult call(TParameter parameter);
}
