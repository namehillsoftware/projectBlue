package com.lasthopesoftware.bluewater.client.playback.state.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;

public interface OnPlaybackStarted {
	void onPlaybackStarted(PositionedPlaybackFile firstPositionedPlaybackFile);
}
