package com.lasthopesoftware.bluewater.client.playback.engine.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

public interface OnPlayingFileChanged {
	void onPlayingFileChanged(PositionedPlayingFile positionedPlayingFile);
}
