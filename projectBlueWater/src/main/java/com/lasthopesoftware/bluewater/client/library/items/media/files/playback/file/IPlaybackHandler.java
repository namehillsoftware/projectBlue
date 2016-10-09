package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.IPromise;

import java.io.Closeable;

public interface IPlaybackHandler extends Closeable {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);

	int getCurrentPosition();

	int getDuration();

	IPromise<IPlaybackHandler> start();
}