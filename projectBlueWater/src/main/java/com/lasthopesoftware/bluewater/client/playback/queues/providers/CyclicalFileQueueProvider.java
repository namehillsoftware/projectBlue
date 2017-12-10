package com.lasthopesoftware.bluewater.client.playback.queues.providers;


import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueue;

import java.util.List;

public class CyclicalFileQueueProvider implements IPositionedFileQueueProvider {
	@Override
	public IPositionedFileQueue provideQueue(List<ServiceFile> playlist, int startingAt) {
		final List<PositionedFile> truncatedList = QueueSplicers.getTruncatedList(playlist, startingAt);

		final int endingPosition = playlist.size() - truncatedList.size();
		for (int i = 0; i < endingPosition; i++)
			truncatedList.add(new PositionedFile(i, playlist.get(i)));

		return new RepeatingPositionedFileQueue(truncatedList);
	}

	@Override
	public boolean isRepeating() {
		return true;
	}
}
