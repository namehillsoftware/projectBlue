package com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackController;

public interface OnPlaylistStateControlErrorListener {
	void onPlaylistStateControlError(PlaybackController controller, IPlaybackFile filePlayer);
}
