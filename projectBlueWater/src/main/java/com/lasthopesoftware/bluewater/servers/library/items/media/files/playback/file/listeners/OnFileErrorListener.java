package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;

public interface OnFileErrorListener {
	void onFileError(IPlaybackFile mediaPlayer, int what, int extra);
}
