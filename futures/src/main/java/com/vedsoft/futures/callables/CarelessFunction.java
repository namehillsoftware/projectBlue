package com.vedsoft.futures.callables;

/**
 * Created by david on 11/6/16.
 */

public interface CarelessFunction<TResult> {
	TResult result() throws Exception;
}
