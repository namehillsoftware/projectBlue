package com.lasthopesoftware.messenger.promises.queued;

public interface MessageTask<Resolution> {
	Resolution prepareMessage() throws Throwable;
}