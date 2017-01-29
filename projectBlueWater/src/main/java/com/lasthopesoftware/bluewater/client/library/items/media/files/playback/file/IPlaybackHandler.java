package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;

import io.reactivex.Observable;

public interface IPlaybackHandler extends Closeable {
	boolean isPlaying();
	void pause();

	void setVolume(float volume);
	float getVolume();

	int getCurrentPosition();

	Observable<Integer> observeCurrentPosition();

	int getDuration();

	IPromise<IPlaybackHandler> promisePlayback();
}