package com.lasthopesoftware.promises;

/**
 * Created by david on 10/23/16.
 */

public interface IResolvedPromise<Resolution> {
	void sendResolution(Resolution resolution);
}
