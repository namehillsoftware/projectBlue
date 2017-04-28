package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.CarelessOneParameterAction;

/**
 * Created by david on 11/8/16.
 */

final class CarelessVoidOneParameterFunction<ParameterOne> implements CarelessOneParameterFunction<ParameterOne, Void> {

	private final CarelessOneParameterAction<ParameterOne> action;

	CarelessVoidOneParameterFunction(CarelessOneParameterAction<ParameterOne> action) {
		this.action = action;
	}

	@Override
	public Void resultFrom(ParameterOne parameterOne) throws Throwable {
		action.runWith(parameterOne);
		return null;
	}
}
