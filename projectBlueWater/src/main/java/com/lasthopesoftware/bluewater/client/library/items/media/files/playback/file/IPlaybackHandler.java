package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.IPlaybackFileErrorBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerErrorData;
import com.vedsoft.fluent.FluentRunnable;

import java.io.Closeable;

public interface IPlaybackHandler extends IPlaybackFileErrorBroadcaster<MediaPlayerErrorData>, Closeable {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);

	int getCurrentPosition();

	int getDuration();

	FluentRunnable start();
	void stop();
}