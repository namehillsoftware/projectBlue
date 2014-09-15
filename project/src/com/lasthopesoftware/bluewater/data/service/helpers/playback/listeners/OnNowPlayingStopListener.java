package com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;

public interface OnNowPlayingStopListener {
	
	/*
	 * Only thrown when the PlaylistController is finished playing and is not set to repeat
	 */
	void onNowPlayingStop(PlaylistController controller, FilePlayer filePlayer);
}
