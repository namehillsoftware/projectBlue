package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface FourParameterRunnable<TFirstParameter, TSecondParameter, TThirdParameter, TFourthParameter> {
	void run(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree, TFourthParameter parameterFour);
}
