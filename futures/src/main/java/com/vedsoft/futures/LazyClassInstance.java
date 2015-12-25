package com.vedsoft.futures;

/**
 * Created by david on 12/25/15.
 */
public class LazyClassInstance<T> extends Lazy<T> {
	private final Class<T> cls;

	public LazyClassInstance(Class<T> cls) {
		this.cls = cls;
	}

	@Override
	public T initialize() {
		try {
			return this.cls.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
