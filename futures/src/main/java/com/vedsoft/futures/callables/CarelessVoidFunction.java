package com.vedsoft.futures.callables;

/**
 * Created by david on 11/8/16.
 */

final class CarelessVoidFunction implements CarelessFunction<Void> {

	private final Runnable action;

	CarelessVoidFunction(Runnable action) {
		this.action = action;
	}

	@Override
	public Void result() throws Throwable {
		action.run();
		return null;
	}
}
