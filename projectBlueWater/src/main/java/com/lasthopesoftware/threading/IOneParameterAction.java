package com.lasthopesoftware.threading;

/**
 * Created by david on 8/15/15.
 */
public interface IOneParameterAction<TParameter> {
	void run(TParameter parameter);
}
