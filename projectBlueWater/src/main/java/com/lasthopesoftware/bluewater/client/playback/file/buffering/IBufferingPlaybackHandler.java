package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 10/23/16.
 */

public interface IBufferingPlaybackHandler extends IPlaybackHandler {
	Promise<IBufferingPlaybackHandler> bufferPlaybackFile();
}
