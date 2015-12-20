package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface ThreeParameterRunnable<TFirstParameter, TSecondParameter, TThirdParameter> {
	void run(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree);
}
