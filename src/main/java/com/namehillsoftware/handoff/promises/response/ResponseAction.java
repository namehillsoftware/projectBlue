package com.namehillsoftware.handoff.promises.response;

public interface ResponseAction<Resolution> {
	void perform(Resolution resolution) throws Throwable;
}
