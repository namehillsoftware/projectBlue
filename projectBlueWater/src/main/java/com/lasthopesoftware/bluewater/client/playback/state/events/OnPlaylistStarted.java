package com.lasthopesoftware.bluewater.client.playback.state.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;

public interface OnPlaylistStarted {
	void onPlaylistStarted(PositionedPlaybackFile firstPositionedPlaybackFile);
}
