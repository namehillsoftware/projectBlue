package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaybackController;

public interface OnNowPlayingPauseListener {
	void onNowPlayingPause(PlaybackController controller, IPlaybackFile filePlayer);
}
