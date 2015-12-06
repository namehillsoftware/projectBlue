package com.lasthopesoftware.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface ITwoParameterRunnable<TFirstParameter, TSecondParameter> {
	void run(TFirstParameter parameterOne, TSecondParameter parameterTwo);
}
