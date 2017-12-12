package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

/**
 * Created by david on 11/16/16.
 */

public interface IPositionedFileQueue {
	PositionedFile poll();
	PositionedFile peek();
}
