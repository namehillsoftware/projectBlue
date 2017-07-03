package com.lasthopesoftware.promises;

public interface Messenger<Resolution> {
	void sendResolution(Resolution resolution);
	void sendRejection(Throwable error);
	void cancellationRequested(Runnable response);
}
