package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */

public interface IPositionedFileQueueProvider {
	IPositionedFileQueue getCompletableQueue(List<File> playlist, int startingAt);
	IPositionedFileQueue getCyclicalQueue(List<File> playlist, int startingAt);
}
