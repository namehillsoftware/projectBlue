package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileProvider {
	IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile();
	IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile(int preparedAt);
}
