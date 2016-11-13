package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

public interface OnFileErrorListener {
	void onFileError(IPlaybackHandler mediaPlayer, int what, int extra);
}
