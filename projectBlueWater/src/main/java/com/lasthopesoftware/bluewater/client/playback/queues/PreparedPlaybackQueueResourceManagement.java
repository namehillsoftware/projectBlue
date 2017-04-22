package com.lasthopesoftware.bluewater.client.playback.queues;


import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;

import java.io.Closeable;
import java.io.IOException;

public class PreparedPlaybackQueueResourceManagement implements Closeable {
	private final IPlaybackPreparerProvider playbackPreparerProvider;

	private PreparedPlaybackQueue preparedPlaybackQueue;

	public PreparedPlaybackQueueResourceManagement(IPlaybackPreparerProvider playbackPreparerProvider) {
		this.playbackPreparerProvider = playbackPreparerProvider;
	}

	public PreparedPlaybackQueue initializePreparedPlaybackQueue(IPositionedFileQueue positionedFileQueue) throws IOException {
		if (preparedPlaybackQueue != null)
			preparedPlaybackQueue.close();

		return
			preparedPlaybackQueue =
				new PreparedPlaybackQueue(
					this.playbackPreparerProvider.providePlaybackPreparer(),
					positionedFileQueue);
	}

	public boolean tryUpdateQueue(IPositionedFileQueue positionedFileQueue) {
		if (preparedPlaybackQueue == null) return false;

		preparedPlaybackQueue.updateQueue(positionedFileQueue);
		return true;
	}

	@Override
	public void close() throws IOException {
		if (preparedPlaybackQueue != null)
			preparedPlaybackQueue.close();
	}
}
