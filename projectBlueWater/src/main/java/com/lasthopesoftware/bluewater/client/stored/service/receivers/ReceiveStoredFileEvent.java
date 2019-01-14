package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import com.namehillsoftware.handoff.promises.Promise;

public interface ReceiveStoredFileEvent {
	Promise<Void> receive(int storedFileId);

	boolean isAcceptable(String event);
}
