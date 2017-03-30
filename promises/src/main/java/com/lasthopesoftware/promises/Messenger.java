package com.lasthopesoftware.promises;

/**
 * Created by david on 3/29/17.
 */

public interface Messenger<Resolution> {
	void sendResolution(Resolution resolution);
	void sendRejection(Throwable rejection);
	void cancellationReceived(Runnable response);
}
