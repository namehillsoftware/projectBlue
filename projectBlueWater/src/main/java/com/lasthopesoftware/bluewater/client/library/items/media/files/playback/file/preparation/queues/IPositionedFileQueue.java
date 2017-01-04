package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;

/**
 * Created by david on 11/16/16.
 */

public interface IPositionedFileQueue {
	PositionedFile poll();
}
