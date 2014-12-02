package com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;

public interface OnFileErrorListener {
	void onFileError(FilePlayer mediaPlayer, int what, int extra);
}
