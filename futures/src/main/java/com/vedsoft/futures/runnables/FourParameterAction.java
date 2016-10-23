package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface FourParameterAction<TFirstParameter, TSecondParameter, TThirdParameter, TFourthParameter> {
	void runWith(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree, TFourthParameter parameterFour);
}
