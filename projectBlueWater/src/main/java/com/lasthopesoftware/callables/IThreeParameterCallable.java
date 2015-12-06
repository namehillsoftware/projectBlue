package com.lasthopesoftware.callables;

/**
 * Created by david on 12/5/15.
 */
public interface IThreeParameterCallable<TFirstParameter, TSecondParameter, TThirdParameter, TResult> {
	TResult call(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree);
}
