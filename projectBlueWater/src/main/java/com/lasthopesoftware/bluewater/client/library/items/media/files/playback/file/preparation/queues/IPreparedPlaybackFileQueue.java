package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackServiceFile;
import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileQueue extends Closeable {
	IPromise<PositionedPlaybackServiceFile> promiseNextPreparedPlaybackFile(int preparedAt);
}
