package com.vedsoft.lazyj;

import java.util.concurrent.Callable;

/**
 * Created by david on 1/5/16.
 */
public final class Lazy<T> extends AbstractSynchronousLazy<T> {

	private final Callable<T> initialization;

	public Lazy(Callable<T> initialization) {
		this.initialization = initialization;
	}

	@Override
	protected final T initialize() throws Exception {
		return this.initialization.call();
	}
}
