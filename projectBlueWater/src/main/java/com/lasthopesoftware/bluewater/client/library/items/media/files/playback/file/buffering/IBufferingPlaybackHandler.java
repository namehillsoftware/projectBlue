package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 10/23/16.
 */

public interface IBufferingPlaybackHandler extends IPlaybackHandler {
	IPromise<IBufferingPlaybackHandler> bufferPlaybackFile();
}
