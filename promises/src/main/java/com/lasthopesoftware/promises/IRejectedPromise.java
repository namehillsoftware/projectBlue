package com.lasthopesoftware.promises;

/**
 * Created by david on 10/23/16.
 */

public interface IRejectedPromise {
	void withError(Exception exception);
}
