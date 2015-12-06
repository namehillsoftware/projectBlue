package com.lasthopesoftware.callables;

/**
 * Created by david on 12/5/15.
 */
public interface ITwoParameterCallable<TFirstParameter, TSecondParameter, TResult> {
	TResult call(TFirstParameter parameterOne, TSecondParameter parameterTwo);
}
