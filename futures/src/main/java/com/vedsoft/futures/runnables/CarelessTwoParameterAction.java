package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface CarelessTwoParameterAction<FirstParameter, SecondParameter> {
	void runWith(FirstParameter parameterOne, SecondParameter parameterTwo) throws Throwable;
}
