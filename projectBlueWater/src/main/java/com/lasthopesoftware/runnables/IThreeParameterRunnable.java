package com.lasthopesoftware.runnables;

/**
 * Created by david on 12/5/15.
 */
public interface IThreeParameterRunnable<TFirstParameter, TSecondParameter, TThirdParameter> {
	void run(TFirstParameter parameterOne, TSecondParameter parameterTwo, TThirdParameter parameterThree);
}
