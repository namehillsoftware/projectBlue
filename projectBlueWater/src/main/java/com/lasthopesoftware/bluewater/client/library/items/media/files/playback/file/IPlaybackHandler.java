package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.IPlaybackFileErrorBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.vedsoft.fluent.IFluentTask;

import java.io.Closeable;

public interface IPlaybackHandler extends IPlaybackFileErrorBroadcaster<MediaPlayerException>, Closeable {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);

	int getCurrentPosition();

	int getDuration();

	IFluentTask<Void, Integer, Void> start();
}