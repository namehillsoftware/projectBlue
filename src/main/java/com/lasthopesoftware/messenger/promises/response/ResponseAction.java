package com.lasthopesoftware.messenger.promises.response;

public interface ResponseAction<Resolution> {
	void perform(Resolution resolution) throws Throwable;
}
