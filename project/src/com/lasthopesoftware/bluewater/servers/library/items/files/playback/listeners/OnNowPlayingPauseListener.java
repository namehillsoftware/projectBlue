package com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.FilePlayer;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackController;

public interface OnNowPlayingPauseListener {
	void onNowPlayingPause(PlaybackController controller, FilePlayer filePlayer);
}
