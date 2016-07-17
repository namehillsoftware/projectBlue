package com.vedsoft.lazyj;

/**
 * Created by david on 7/17/16.
 */
public interface ILazy<T> {
	boolean isInitialized();
	T getObject();
}
