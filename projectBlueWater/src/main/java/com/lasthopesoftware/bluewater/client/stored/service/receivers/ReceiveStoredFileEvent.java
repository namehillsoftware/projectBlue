package com.lasthopesoftware.bluewater.client.stored.service.receivers;

public interface ReceiveStoredFileEvent {
	void receive(int storedFileId);

	boolean isAcceptable(String event);
}
