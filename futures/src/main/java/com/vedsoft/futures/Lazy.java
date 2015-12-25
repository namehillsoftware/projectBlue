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

	protected abstract T initialize();

	public static <T> Lazy<T> fromClass(final Class<T> cls) {
		return new Lazy<T>() {
			@Override
			protected T initialize() {
				try {
					return cls.newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
