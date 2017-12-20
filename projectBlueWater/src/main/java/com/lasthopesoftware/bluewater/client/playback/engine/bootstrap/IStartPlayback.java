package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap;

import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;

import java.io.IOException;

public interface IStartPlayback {
	IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, final long filePosition) throws IOException;
}
