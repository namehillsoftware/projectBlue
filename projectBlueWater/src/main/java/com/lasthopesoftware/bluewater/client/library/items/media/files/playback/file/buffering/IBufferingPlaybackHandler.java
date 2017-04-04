package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.Promise;

/**
 * Created by david on 10/23/16.
 */

public interface IBufferingPlaybackHandler extends IPlaybackHandler {
	Promise<IBufferingPlaybackHandler> bufferPlaybackFile();
}
