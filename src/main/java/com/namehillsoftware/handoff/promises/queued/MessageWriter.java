package com.namehillsoftware.handoff.promises.queued;

public interface MessageWriter<Resolution> {
	Resolution prepareMessage() throws Throwable;
}