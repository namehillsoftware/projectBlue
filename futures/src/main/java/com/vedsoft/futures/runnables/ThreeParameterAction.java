package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface ThreeParameterAction<TFirstParameter, TSecondParameter, TThirdParameter> {
	void runWith(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree);
}
