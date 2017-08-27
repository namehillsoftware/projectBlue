package com.lasthopesoftware.messenger.promises.queued;

public interface MessageWriter<Resolution> {
	Resolution prepareMessage() throws Throwable;
}