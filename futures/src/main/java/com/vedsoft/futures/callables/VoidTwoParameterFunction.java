package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 11/17/16.
 */
final class VoidTwoParameterFunction<ParameterOne, ParameterTwo> implements TwoParameterFunction<ParameterOne, ParameterTwo, Void> {
	private final TwoParameterAction<ParameterOne, ParameterTwo> action;

	VoidTwoParameterFunction(TwoParameterAction<ParameterOne, ParameterTwo> action) {
		this.action = action;
	}

	@Override
	public Void resultFrom(ParameterOne paramOne, ParameterTwo paramTwo) {
		action.runWith(paramOne, paramTwo);
		return null;
	}
}
