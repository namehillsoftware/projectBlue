package com.vedsoft.futures;

/**
 * Created by david on 11/28/15.
 */
public abstract class Lazy<T> {

	private T object;

	public boolean isInitialized() {
		return object != null;
	}

	public T getObject() {
		return isInitialized() ? object : getValueSynchronized();
	}

	private synchronized T getValueSynchronized() {
		if (!isInitialized())
			object = initialize();

		return object;
	}

	public abstract T initialize();
}
