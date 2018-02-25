package com.lasthopesoftware.bluewater.client.playback.engine.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;

public interface OnPlaybackStarted {
	void onPlaybackStarted(PositionedPlayableFile firstPositionedPlayableFile);
}
