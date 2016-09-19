package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileCompleteListener;

/**
 * Created by david on 9/18/16.
 */
public interface IPlaybackFileController {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);
	void start();
	void stop();
	void setOnFileCompleteListener(OnFileCompleteListener listener);
}
