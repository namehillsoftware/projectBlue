package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue;

import java.io.Closeable;
import java.io.IOException;

public interface ManagePlaybackQueues extends Closeable {
	PreparedPlayableFileQueue initializePreparedPlaybackQueue(PositionedFileQueue positionedFileQueue) throws IOException;

	boolean tryUpdateQueue(PositionedFileQueue positionedFileQueue);
}
