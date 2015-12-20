package com.vedsoft.futures.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface TwoParameterRunnable<TFirstParameter, TSecondParameter> {
	void run(TFirstParameter parameterOne, TSecondParameter parameterTwo);
}
