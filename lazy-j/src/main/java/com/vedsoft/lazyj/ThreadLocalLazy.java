package com.vedsoft.lazyj;

import java.util.concurrent.Callable;

/**
 * Created by david on 7/17/16.
 */
public class ThreadLocalLazy<T> extends AbstractThreadLocalLazy<T> {

	private final Callable<T> initialization;

	public ThreadLocalLazy(final Class<T> cls) {
		this(cls::newInstance);
	}

	public ThreadLocalLazy(Callable<T> initialization) {
		this.initialization = initialization;
	}

	@Override
	protected final T initialize() throws Exception {
		return this.initialization.call();
	}
}
