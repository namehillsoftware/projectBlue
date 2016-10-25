package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileProvider extends Closeable {
	IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile();
	IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile(int preparedAt);
}
