package com.vedsoft.lazyj;

/**
 * Created by david on 1/5/16.
 */
public class LazyNewInstance<T> extends Lazy<T> {

	private final Class<T> cls;

	public LazyNewInstance(Class<T> cls) {
		this.cls = cls;
	}

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
}
