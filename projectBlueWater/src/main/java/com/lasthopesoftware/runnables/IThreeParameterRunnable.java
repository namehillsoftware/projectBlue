package com.lasthopesoftware.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface IThreeParameterRunnable<TFirstParameter, TSecondParameter, TThreeParameterRunnable> {
	void run(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThreeParameterRunnable parameterThree);
}
