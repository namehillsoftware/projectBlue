package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;

public interface IPlaybackHandler extends Closeable {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);
	void setVolume(float volume);
	float getVolume();


	int getCurrentPosition();

	int getDuration();

	IPromise<IPlaybackHandler> promisePlayback();
}