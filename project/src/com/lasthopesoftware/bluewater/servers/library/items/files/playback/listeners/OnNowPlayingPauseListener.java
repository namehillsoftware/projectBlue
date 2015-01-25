package com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.FilePlayer;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.PlaybackListController;

public interface OnNowPlayingPauseListener {
	void onNowPlayingPause(PlaybackListController controller, FilePlayer filePlayer);
}
