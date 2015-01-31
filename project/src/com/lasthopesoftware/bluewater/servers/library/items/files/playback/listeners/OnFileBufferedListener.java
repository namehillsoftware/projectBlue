package com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFile;

public interface OnFileBufferedListener {
	void onFileBuffered(IPlaybackFile filePlayer);
}
