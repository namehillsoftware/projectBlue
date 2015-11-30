package com.lasthopesoftware.threading;

import java.util.concurrent.Callable;

/**
 * Created by david on 11/28/15.
 */
public class Lazy<T> {

	private final Callable<T> initialization;

	private T object;

	public Lazy(Callable<T> initialization) {
		this.initialization = initialization;
	}

	public T getObject() {
		return object != null ? object : getValueSynchronized();
	}

	private synchronized T getValueSynchronized() {
		if (object != null) return object;

		try {
			object = initialization.call();
		} catch (Exception exception) {
			Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception);
		}

		return object;
	}
}
