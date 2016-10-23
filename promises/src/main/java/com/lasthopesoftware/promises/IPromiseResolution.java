package com.lasthopesoftware.promises;

/**
 * Created by david on 10/23/16.
 */

public interface IPromiseResolution<TResult> {
	void fulfilled(TResult result);
	void rejected(Exception error);
}
