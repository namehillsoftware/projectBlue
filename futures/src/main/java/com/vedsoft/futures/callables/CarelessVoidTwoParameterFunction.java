package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.CarelessTwoParameterAction;

/**
 * Created by david on 11/17/16.
 */
final class CarelessVoidTwoParameterFunction<ParameterOne, ParameterTwo> implements CarelessTwoParameterFunction<ParameterOne, ParameterTwo, Void> {
	private final CarelessTwoParameterAction<ParameterOne, ParameterTwo> action;

	CarelessVoidTwoParameterFunction(CarelessTwoParameterAction<ParameterOne, ParameterTwo> action) {
		this.action = action;
	}

	@Override
	public Void resultFrom(ParameterOne paramOne, ParameterTwo paramTwo) throws Throwable {
		action.runWith(paramOne, paramTwo);
		return null;
	}
}
