package com.vedsoft.lazyj;

public interface ILazy<T> {
	boolean isInitialized();
	T getObject();
}
