package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 11/17/16.
 */

public final class VoidFunc {
	private VoidFunc() {}

	public static <ParameterOne> OneParameterFunction<ParameterOne, Void> running(OneParameterAction<ParameterOne> action) {
		return new VoidOneParameterFunction<>(action);
	}

	public static <ParameterOne, ParameterTwo> TwoParameterFunction<ParameterOne, ParameterTwo, Void> running(final TwoParameterAction<ParameterOne, ParameterTwo> action) {
		return new VoidTwoParameterFunction<>(action);
	}

	public static CarelessFunction<Void> runCarelessly(Runnable action) {
		return new CarelessVoidFunction<>(action);
	}

	public static <ParameterOne> CarelessOneParameterFunction<ParameterOne, Void> runCarelessly(OneParameterAction<ParameterOne> action) {
		return new CarelessVoidOneParameterFunction<>(action);
	}

	public static <ParameterOne, ParameterTwo> CarelessTwoParameterFunction<ParameterOne, ParameterTwo, Void> runCarelessly(TwoParameterAction<ParameterOne, ParameterTwo> action) {
		return new CarelessVoidTwoParameterFunction<>(action);
	}
}
