package com.vedsoft.lazyj;

import java.util.concurrent.Callable;

/**
 * Created by david on 1/5/16.
 */
public final class Lazy<T> extends AbstractLazy<T> {

	private final Callable<T> initialization;

	public Lazy(final Class<T> cls) {
		this(cls::newInstance);
	}

	public Lazy(Callable<T> initialization) {
		this.initialization = initialization;
	}

	@Override
	protected final T initialize() throws Exception {
		return this.initialization.call();
	}
}
