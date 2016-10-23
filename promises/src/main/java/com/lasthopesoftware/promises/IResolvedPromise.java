package com.lasthopesoftware.promises;

/**
 * Created by david on 10/23/16.
 */

public interface IResolvedPromise<TResult> {
	void withResult(TResult result);
}
