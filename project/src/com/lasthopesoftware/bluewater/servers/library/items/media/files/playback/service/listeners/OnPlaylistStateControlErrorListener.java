package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;

public interface OnPlaylistStateControlErrorListener {
	void onPlaylistStateControlError(PlaybackController controller, IPlaybackFile filePlayer);
}
