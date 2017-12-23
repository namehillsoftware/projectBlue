package com.lasthopesoftware.bluewater.client.playback.engine.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

public interface OnPlaylistReset {
	void onPlaylistReset(PositionedFile positionedFile);
}
