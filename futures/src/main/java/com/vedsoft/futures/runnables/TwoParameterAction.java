package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface TwoParameterAction<TFirstParameter, TSecondParameter> {
	void runWith(TFirstParameter parameterOne, TSecondParameter parameterTwo);
}
