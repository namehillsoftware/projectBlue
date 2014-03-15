package com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrPlaylistController;

public interface OnNowPlayingStartListener {
	void onNowPlayingStart(JrPlaylistController controller, JrFilePlayer filePlayer);
}
