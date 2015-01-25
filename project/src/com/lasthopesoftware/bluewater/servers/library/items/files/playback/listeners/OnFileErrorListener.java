package com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.FilePlayer;

public interface OnFileErrorListener {
	void onFileError(FilePlayer mediaPlayer, int what, int extra);
}
