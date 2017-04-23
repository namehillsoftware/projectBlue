package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;

import java.io.IOException;

/**
 * Created by david on 4/9/17.
 */

public interface IStartPlayback {
	IActivePlayer startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException;
}
