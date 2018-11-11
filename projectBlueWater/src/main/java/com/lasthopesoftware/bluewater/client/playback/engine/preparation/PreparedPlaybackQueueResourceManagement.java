package com.lasthopesoftware.bluewater.client.playback.engine.preparation;


import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;

import java.io.Closeable;

public class PreparedPlaybackQueueResourceManagement implements Closeable {
	private final IPlayableFilePreparationSourceProvider playbackPreparerProvider;
	private final IPreparedPlaybackQueueConfiguration preparedPlaybackQueueConfiguration;

	private PreparedPlayableFileQueue preparedPlaybackQueue;

	public PreparedPlaybackQueueResourceManagement(IPlayableFilePreparationSourceProvider playbackPreparerProvider, IPreparedPlaybackQueueConfiguration preparedPlaybackQueueConfiguration) {
		this.playbackPreparerProvider = playbackPreparerProvider;
		this.preparedPlaybackQueueConfiguration = preparedPlaybackQueueConfiguration;
	}

	public PreparedPlayableFileQueue initializePreparedPlaybackQueue(IPositionedFileQueue positionedFileQueue) {
		close();

		return preparedPlaybackQueue =
			new PreparedPlayableFileQueue(
				preparedPlaybackQueueConfiguration,
				this.playbackPreparerProvider.providePlayableFilePreparationSource(),
				positionedFileQueue);
	}

	public boolean tryUpdateQueue(IPositionedFileQueue positionedFileQueue) {
		if (preparedPlaybackQueue == null) return false;

		preparedPlaybackQueue.updateQueue(positionedFileQueue);
		return true;
	}

	@Override
	public void close() {
		if (preparedPlaybackQueue != null)
			preparedPlaybackQueue.close();
	}
}
