package com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.FilePlayer;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.PlaybackListController;

public interface OnNowPlayingChangeListener {
	void onNowPlayingChange(PlaybackListController controller, FilePlayer filePlayer);
}
