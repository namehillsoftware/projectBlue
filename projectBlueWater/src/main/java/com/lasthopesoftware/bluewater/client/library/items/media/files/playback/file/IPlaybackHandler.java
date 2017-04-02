package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.promises.Promise;

import java.io.Closeable;

public interface IPlaybackHandler extends Closeable {
	boolean isPlaying();
	void pause();

	void setVolume(float volume);
	float getVolume();

	int getCurrentPosition();

	int getDuration();

	Promise<IPlaybackHandler> promisePlayback();
}