package com.vedsoft.lazyj;

/**
 * Created by david on 7/17/16.
 */
public abstract class AbstractThreadLocalLazy<T> implements ILazy<T> {
	private final ThreadLocal<T> threadLocalObjectContainer = new ThreadLocal<>();

	private RuntimeException exception;

	public boolean isInitialized() {
		return exception != null || threadLocalObjectContainer.get() != null;
	}

	public T getObject() {
		if (!isInitialized()) {
			try {
				threadLocalObjectContainer.set(initialize());
			} catch (Exception e) {
				exception = new RuntimeException(e);
			}
		}

		if (exception != null)
			throw exception;

		return threadLocalObjectContainer.get();
	}

	protected abstract T initialize() throws Exception;
}
