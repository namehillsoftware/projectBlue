package com.lasthopesoftware.bluewater.client.playback.engine.preparation;


import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;

import java.io.Closeable;
import java.io.IOException;

public class PreparedPlaybackQueueResourceManagement implements Closeable {
	private final IPlaybackPreparerProvider playbackPreparerProvider;
	private final IPreparedPlaybackQueueConfiguration preparedPlaybackQueueConfiguration;

	private PreparedPlaybackQueue preparedPlaybackQueue;

	public PreparedPlaybackQueueResourceManagement(IPlaybackPreparerProvider playbackPreparerProvider, IPreparedPlaybackQueueConfiguration preparedPlaybackQueueConfiguration) {
		this.playbackPreparerProvider = playbackPreparerProvider;
		this.preparedPlaybackQueueConfiguration = preparedPlaybackQueueConfiguration;
	}

	public PreparedPlaybackQueue initializePreparedPlaybackQueue(IPositionedFileQueue positionedFileQueue) throws IOException {
		close();

		return
			preparedPlaybackQueue =
				new PreparedPlaybackQueue(
					preparedPlaybackQueueConfiguration,
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
