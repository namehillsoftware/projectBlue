package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.Closeable;

public interface IPreparedPlaybackQueue extends Closeable {
	Promise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(long preparedAt);
}
