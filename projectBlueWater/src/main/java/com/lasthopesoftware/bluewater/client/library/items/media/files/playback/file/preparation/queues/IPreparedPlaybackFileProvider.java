package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedPlaybackHandlerContainer;
import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileProvider extends Closeable {
	IPromise<PositionedPlaybackHandlerContainer> promiseNextPreparedPlaybackFile(int preparedAt);
}
