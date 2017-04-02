package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackServiceFile;
import com.lasthopesoftware.promises.Promise;

import java.io.Closeable;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileQueue extends Closeable {
	Promise<PositionedPlaybackServiceFile> promiseNextPreparedPlaybackFile(int preparedAt);
}
