package com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrPlaylistController;

public interface OnPlaylistStateControlErrorListener {
	boolean onPlaylistStateControlError(JrPlaylistController controller, JrFilePlayer filePlayer);
}
