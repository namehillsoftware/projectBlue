package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;

public interface IPreparedPlaybackFile {
	IBufferingPlaybackFile getBufferingPlaybackFile();
	IPlaybackHandler getPlaybackHandler();
}
