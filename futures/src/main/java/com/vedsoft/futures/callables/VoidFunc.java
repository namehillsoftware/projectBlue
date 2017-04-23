package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.CarelessOneParameterAction;
import com.vedsoft.futures.runnables.CarelessTwoParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 11/17/16.
 */

public final class VoidFunc {
	private VoidFunc() {}

	public static <ParameterOne> OneParameterFunction<ParameterOne, Void> run(OneParameterAction<ParameterOne> action) {
		return new VoidOneParameterFunction<>(action);
	}

	public static <ParameterOne, ParameterTwo> TwoParameterFunction<ParameterOne, ParameterTwo, Void> run(final TwoParameterAction<ParameterOne, ParameterTwo> action) {
		return new VoidTwoParameterFunction<>(action);
	}

	public static CarelessFunction<Void> runCarelessly(Runnable action) {
		return new CarelessVoidFunction(action);
	}

	public static <ParameterOne> CarelessOneParameterFunction<ParameterOne, Void> runCarelessly(CarelessOneParameterAction<ParameterOne> action) {
		return new CarelessVoidOneParameterFunction<>(action);
	}

	public static <ParameterOne, ParameterTwo> CarelessTwoParameterFunction<ParameterOne, ParameterTwo, Void> runCarelessly(CarelessTwoParameterAction<ParameterOne, ParameterTwo> action) {
		return new CarelessVoidTwoParameterFunction<>(action);
	}
}
