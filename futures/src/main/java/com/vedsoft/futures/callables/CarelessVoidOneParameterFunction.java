package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.OneParameterAction;

/**
 * Created by david on 11/8/16.
 */

final class CarelessVoidOneParameterFunction<ParameterOne> implements CarelessOneParameterFunction<ParameterOne, Void> {

	private final OneParameterAction<ParameterOne> action;

	CarelessVoidOneParameterFunction(OneParameterAction<ParameterOne> action) {
		this.action = action;
	}

	@Override
	public Void resultFrom(ParameterOne parameterOne) {
		action.runWith(parameterOne);
		return null;
	}
}
