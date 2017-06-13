package com.lasthopesoftware.promises;

public interface IResolvedPromise<Resolution> {
	void sendResolution(Resolution resolution);
}
