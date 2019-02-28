package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;

import java.io.Closeable;
import java.io.IOException;

public interface ManagePlaybackQueues extends Closeable {
	PreparedPlayableFileQueue initializePreparedPlaybackQueue(IPositionedFileQueue positionedFileQueue) throws IOException;

	boolean tryUpdateQueue(IPositionedFileQueue positionedFileQueue);
}
