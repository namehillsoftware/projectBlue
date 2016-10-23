package com.vedsoft.futures.runnables;

/**
 * Created by david on 8/15/15.
 */
public interface OneParameterAction<TParameter> {
	void runWith(TParameter parameter);
}
