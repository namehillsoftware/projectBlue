package com.lasthopesoftware.bluewater.client.playback.engine.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

public interface OnPlaybackStarted {
	void onPlaybackStarted(PositionedPlayingFile firstPositionedPlayingFile);
}
