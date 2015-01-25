package com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.FilePlayer;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.PlaybackListController;

public interface OnNowPlayingStopListener {
	
	/*
	 * Only thrown when the PlaylistController is finished playing and is not set to repeat
	 */
	void onNowPlayingStop(PlaybackListController controller, FilePlayer filePlayer);
}
