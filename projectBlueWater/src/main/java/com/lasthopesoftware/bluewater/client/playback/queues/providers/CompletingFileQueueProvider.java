package com.lasthopesoftware.bluewater.client.playback.queues.providers;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueue;

import java.util.List;

public class CompletingFileQueueProvider implements IPositionedFileQueueProvider {

	@Override
	public IPositionedFileQueue provideQueue(List<ServiceFile> playlist, int startingAt) {
		return new CompletingPositionedFileQueue(QueueSplicers.getTruncatedList(playlist, startingAt));
	}

	@Override
	public boolean isRepeating() {
		return false;
	}
}
