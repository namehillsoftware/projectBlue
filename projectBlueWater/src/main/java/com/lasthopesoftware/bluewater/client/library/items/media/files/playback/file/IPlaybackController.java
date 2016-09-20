package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnPlaybackCompleteListener;

/**
 * Created by david on 9/18/16.
 */
public interface IPlaybackController {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);
	int getCurrentPosition();
	int getDuration();
	void start();
	void stop();
	void setOnPlaybackCompleteListener(OnPlaybackCompleteListener listener);
}
