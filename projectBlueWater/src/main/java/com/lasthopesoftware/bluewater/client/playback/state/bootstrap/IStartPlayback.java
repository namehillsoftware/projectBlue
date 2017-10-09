package com.lasthopesoftware.bluewater.client.playback.state.bootstrap;

import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.state.IActivePlayer;

import java.io.IOException;

/**
 * Created by david on 4/9/17.
 */

public interface IStartPlayback {
	IActivePlayer startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final long filePosition) throws IOException;
}
