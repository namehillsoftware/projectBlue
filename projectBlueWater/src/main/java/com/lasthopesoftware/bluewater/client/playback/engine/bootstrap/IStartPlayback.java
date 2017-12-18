package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap;

import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueue;

import java.io.IOException;

/**
 * Created by david on 4/9/17.
 */

public interface IStartPlayback {
	IActivePlayer startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final long filePosition) throws IOException;
}
