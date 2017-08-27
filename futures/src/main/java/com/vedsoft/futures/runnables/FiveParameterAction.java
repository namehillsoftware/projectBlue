package com.vedsoft.futures.runnables;

/**
 * Created by david on 10/25/16.
 */

public interface FiveParameterAction<TFirstParameter, TSecondParameter, TThirdParameter, TFourthParameter, TFifthParameter> {
	void runWith(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree, TFourthParameter parameterFour, TFifthParameter parameterFive);
}
