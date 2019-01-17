package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface ReceiveStoredFileEvent {
	Promise<Void> receive(int storedFileId);

	Collection<String> acceptedEvents();
}
