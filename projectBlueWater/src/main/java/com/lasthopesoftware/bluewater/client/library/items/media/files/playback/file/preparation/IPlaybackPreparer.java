package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/6/16.
 */

public interface IPlaybackPreparer {
	IPromise<IBufferingPlaybackHandler> promisePreparedPlaybackHandler(File file, int preparedAt);
}
