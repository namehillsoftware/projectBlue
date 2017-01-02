package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;

/**
 * Created by david on 9/26/16.
 */

public interface IMutablePreparedPlaybackFileQueue extends IPreparedPlaybackFileQueue {
	IMutablePreparedPlaybackFileQueue add(File file);
	IMutablePreparedPlaybackFileQueue insert(PositionedFile positionedFile);
	IMutablePreparedPlaybackFileQueue remove(PositionedFile positionedFile);
}
